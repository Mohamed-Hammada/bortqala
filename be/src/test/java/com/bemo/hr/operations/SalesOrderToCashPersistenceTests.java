package com.bemo.hr.operations;

import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.FiscalPeriod;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.posting.PostingProfile;
import com.bemo.hr.finance.domain.posting.PostingProfileLine;
import com.bemo.hr.finance.domain.posting.PostingProfileLineRepository;
import com.bemo.hr.finance.domain.posting.PostingProfileRepository;
import com.bemo.hr.finance.infrastructure.*;
import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.operations.infrastructure.StockStatusBalanceRepository;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.sales.api.SalesApi;
import com.bemo.hr.trade.sales.application.SalesOrderFullService;
import com.bemo.hr.trade.sales.application.SalesReceivablesService;
import com.bemo.hr.trade.sales.domain.CustomerInvoice;
import com.bemo.hr.trade.sales.domain.CustomerReceipt;
import com.bemo.hr.trade.sales.infrastructure.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SalesOrderToCashPersistenceTests {
    @Autowired
    TenantApplicationRepository tenantApplicationRepository;
    @Autowired
    OperationsService operationsService;
    @Autowired
    InventoryValuationService inventoryValuationService;
    @Autowired
    WarehouseInventoryService warehouseInventoryService;
    @Autowired
    WarehouseRepository warehouseRepository;
    @Autowired
    BranchRepository branchRepository;
    @Autowired
    CompanyRepository companyRepository;
    @Autowired
    StockStatusBalanceRepository stockStatusBalanceRepository;
    @Autowired
    SalesOrderFullService salesOrderFullService;
    @Autowired
    SalesReceivablesService salesReceivablesService;
    @Autowired
    SalesPricingSnapshotRepository pricingSnapshotRepository;
    @Autowired
    SalesDeliveryLineRepository deliveryLineRepository;
    @Autowired
    CustomerReturnLineRepository returnLineRepository;
    @Autowired
    CustomerCreditNoteRepository creditNoteRepository;
    @Autowired
    CustomerInvoiceRepository invoiceRepository;
    @Autowired
    CustomerReceiptRepository receiptRepository;
    @Autowired
    PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Autowired
    InventoryMovementCostRepository movementCostRepository;
    @Autowired
    JournalEntryRepository journalEntryRepository;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired
    PostingProfileRepository postingProfileRepository;
    @Autowired
    PostingProfileLineRepository postingProfileLineRepository;
    @Autowired
    JournalEntryLineRepository journalEntryLineRepository;
    @Autowired
    JournalSourceMetadataRepository journalSourceMetadataRepository;
    @Autowired
    BusinessPartyRepository businessPartyRepository;

    private static long ms(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    @BeforeEach
    void tenant() {
        TenantContext.set(tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue("TEST").orElseThrow().getId());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void orderReserveDeliverInvoiceReceiptsReturnCreditKeepsStockArAndGlTraceable() {
        String suffix = Long.toString(System.nanoTime());
        LocalDate today = LocalDate.now();
        if (fiscalPeriodRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(today, today, List.of(FiscalPeriod.Status.OPEN)).isEmpty())
            fiscalPeriodRepository.save(new FiscalPeriod(today.getYear(), today.getMonthValue(), today.getMonth().name(), today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), FiscalPeriod.Status.OPEN));
        Account inventory = accountRepository.save(new Account("I" + suffix, "Inventory", Account.Type.ASSET, null, false, "EGP", true));
        Account receiptOffset = accountRepository.save(new Account("R" + suffix, "Receipt offset", Account.Type.LIABILITY, null, false, "EGP", true));
        Account cogs = accountRepository.save(new Account("C" + suffix, "COGS", Account.Type.EXPENSE, null, false, "EGP", true));
        Account adjustment = accountRepository.save(new Account("A" + suffix, "Adjustment", Account.Type.EXPENSE, null, false, "EGP", true));
        Account receivable = accountRepository.save(new Account("AR" + suffix, "Accounts receivable", Account.Type.ASSET, null, false, "EGP", true));
        Account revenue = accountRepository.save(new Account("REV" + suffix, "Sales revenue", Account.Type.REVENUE, null, false, "EGP", true));
        Account cash = accountRepository.save(new Account("CASH" + suffix, "Cash", Account.Type.ASSET, null, false, "EGP", true));
        postingProfile("CUSTOMER_INVOICE_ISSUED", receivable.getId(), revenue.getId(), today);
        postingProfile("CUSTOMER_RECEIPT_RECORDED", cash.getId(), receivable.getId(), today);
        postingProfile("CUSTOMER_CREDIT_NOTE_ISSUED", revenue.getId(), receivable.getId(), today);
        inventoryValuationService.updatePolicy(new OperationsApi.ValuationPolicyRequest(InventoryValuationPolicy.Method.WEIGHTED_AVERAGE,
                inventory.getId(), receiptOffset.getId(), cogs.getId(), adjustment.getId(), true, false, null), "admin");
        var item = operationsService.createItem(new OperationsApi.ItemRequest("O2C-" + suffix, "O2C Item", "PRODUCT", "EA", null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, null));
        var warehouse = warehouseInventoryService.createWarehouse(createBranch(suffix).getId(), "O2C-WH-" + suffix, "O2C warehouse", null);
        BusinessParty customer = businessPartyRepository.save(new BusinessParty("CUST-" + suffix, "O2C Customer", null, "PROCESSING_CUSTOMER", null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "PER_DELIVERY", "NET_30", null, null));
        operationsService.recordGoodsReceipt(item.id(), null, warehouse.getId(), new BigDecimal("10"), new BigDecimal("60"), "OPEN-" + suffix, null, today.atStartOfDay(ZoneOffset.UTC).toInstant(), "admin");

        SalesApi.SalesOrderResponse order = salesOrderFullService.createOrder(new SalesApi.SalesOrderPayload("SO-" + suffix, ms(today), customer.getId(), null, null, warehouse.getId(), "EGP",
                List.of(new SalesApi.SalesOrderLineRequest(item.id(), item.name(), new BigDecimal("2"), new BigDecimal("100"), BigDecimal.ZERO))), "seller");
        SalesApi.SalesOrderResponse confirmed = salesOrderFullService.confirmOrder(order.id(), "seller");
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(pricingSnapshotRepository.findBySalesOrderId(order.id())).singleElement().satisfies(row -> assertThat(row.getNetPrice()).isEqualByComparingTo("100"));
        assertThat(warehouseInventoryService.getAvailableStock(warehouse.getId(), item.id())).isEqualByComparingTo("8");

        SalesApi.DeliveryResponse delivery = salesOrderFullService.deliver(order.id(), new SalesApi.DeliveryRequest("DN-" + suffix, ms(today), "deliver-" + suffix), "shipper");
        SalesApi.DeliveryResponse replay = salesOrderFullService.deliver(order.id(), new SalesApi.DeliveryRequest("OTHER", ms(today), "deliver-" + suffix), "shipper");
        assertThat(replay.id()).isEqualTo(delivery.id());
        assertThat(delivery.invoiceId()).isNotBlank();
        assertThat(deliveryLineRepository.findByDeliveryIdOrderByCreatedAtAsc(delivery.id())).singleElement().satisfies(row -> {
            assertThat(row.getCogsAmount()).isEqualByComparingTo("120");
            assertThat(row.getStockMovementId()).isNotBlank();
        });
        assertThat(warehouseInventoryService.getAvailableStock(warehouse.getId(), item.id())).isEqualByComparingTo("8");
        CustomerInvoice invoice = invoiceRepository.findById(delivery.invoiceId()).orElseThrow();
        assertThat(invoice.getOutstandingAmount()).isEqualByComparingTo("200");
        assertThat(invoice.getStatus()).isEqualTo(CustomerInvoice.Status.ISSUED);

        salesReceivablesService.recordReceipt(new SalesApi.ReceiptRequest("RC1-" + suffix, customer.getId(), ms(today), "EGP", new BigDecimal("100"), "receipt-1-" + suffix, List.of(new SalesApi.AllocationRequest(invoice.getId(), new BigDecimal("100")))), "cashier");
        salesReceivablesService.recordReceipt(new SalesApi.ReceiptRequest("RC2-" + suffix, customer.getId(), ms(today), "EGP", new BigDecimal("100"), "receipt-2-" + suffix, List.of(new SalesApi.AllocationRequest(invoice.getId(), new BigDecimal("100")))), "cashier");
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getOutstandingAmount()).isZero();
        assertThat(receiptRepository.count()).isGreaterThanOrEqualTo(2);

        SalesApi.DeliveryLineResponse deliveredLine = delivery.lines().get(0);
        SalesApi.ReturnResponse returned = salesOrderFullService.receiveReturn(order.id(), new SalesApi.ReturnRequest("RET-" + suffix, delivery.id(), ms(today), "Customer return", "return-" + suffix,
                List.of(new SalesApi.ReturnLineRequest(deliveredLine.id(), BigDecimal.ONE, "AVAILABLE"))), "returns");
        SalesApi.ReturnResponse returnReplay = salesOrderFullService.receiveReturn(order.id(), new SalesApi.ReturnRequest("OTHER", delivery.id(), ms(today), "Replay", "return-" + suffix, List.of(new SalesApi.ReturnLineRequest(deliveredLine.id(), BigDecimal.ONE, "AVAILABLE"))), "returns");
        assertThat(returnReplay.id()).isEqualTo(returned.id());
        assertThat(returned.creditNoteId()).isNotBlank();
        assertThat(returnLineRepository.findByReturnIdOrderByCreatedAtAsc(returned.id())).singleElement().satisfies(row -> {
            assertThat(row.getCreditAmount()).isEqualByComparingTo("100");
            assertThat(row.getCogsAmount()).isEqualByComparingTo("60");
        });
        assertThat(creditNoteRepository.findById(returned.creditNoteId())).isPresent();
        assertThat(warehouseInventoryService.getAvailableStock(warehouse.getId(), item.id())).isEqualByComparingTo("9");
        assertThat(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(customer.getId())).extracting(PartnerLedgerEntry::getAmountDelta)
                .containsExactlyInAnyOrder(new BigDecimal("200.00"), new BigDecimal("-100.00"), new BigDecimal("-100.00"), new BigDecimal("-100.00"));
        assertThat(movementCostRepository.findByItemIdOrderByOccurredAtAsc(item.id())).hasSize(3).allSatisfy(cost -> assertThat(cost.getJournalEntryId()).isNotBlank());
        assertThat(journalEntryRepository.count()).isGreaterThanOrEqualTo(3);
        assertPostedSource("CUSTOMER_INVOICE", invoice.getId(), receivable.getId(), revenue.getId(), "200.00");
        CustomerReceipt firstReceipt = receiptRepository.findByOperationId("receipt-1-" + suffix).orElseThrow();
        CustomerReceipt secondReceipt = receiptRepository.findByOperationId("receipt-2-" + suffix).orElseThrow();
        assertPostedSource("CUSTOMER_RECEIPT", firstReceipt.getId(), cash.getId(), receivable.getId(), "100.00");
        assertPostedSource("CUSTOMER_RECEIPT", secondReceipt.getId(), cash.getId(), receivable.getId(), "100.00");
        assertPostedSource("CUSTOMER_CREDIT_NOTE", returned.creditNoteId(), revenue.getId(), receivable.getId(), "100.00");

        String originalTenant = TenantContext.require();
        TenantApplication other = tenantApplicationRepository.save(new TenantApplication("O2C-ISO-" + suffix, "O2C isolation"));
        TenantContext.set(other.getId());
        assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
        assertThat(salesOrderFullService.orders()).isEmpty();
        assertThat(salesOrderFullService.deliveries(order.id())).isEmpty();
        TenantContext.set(originalTenant);
        tenantApplicationRepository.deleteById(other.getId());
    }

    @Test
    void concurrentReservationsCannotOversubscribeAvailableStock() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String tenant = TenantContext.require();
        var item = operationsService.createItem(new OperationsApi.ItemRequest("RSV-" + suffix, "Reserved Item", "PRODUCT", "EA", null, null, BigDecimal.ZERO, BigDecimal.ZERO, true, null));
        var warehouse = warehouseInventoryService.createWarehouse(createBranch(suffix).getId(), "RSV-WH-" + suffix, "Reservation warehouse", null);
        warehouseInventoryService.receiveAvailableStock(warehouse.getId(), item.id(), new BigDecimal("10"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Boolean> first = () -> reserveConcurrently(tenant, barrier, "SO-A-" + suffix, item.id(), warehouse.getId());
        Callable<Boolean> second = () -> reserveConcurrently(tenant, barrier, "SO-B-" + suffix, item.id(), warehouse.getId());
        List<Future<Boolean>> futures = pool.invokeAll(List.of(first, second));
        pool.shutdown();
        assertThat(futures).extracting(future -> {
            try {
                return future.get();
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }).containsExactlyInAnyOrder(true, false);
        assertThat(warehouseInventoryService.getAvailableStock(warehouse.getId(), item.id())).isEqualByComparingTo("3");
    }

    private boolean reserveConcurrently(String tenant, CyclicBarrier barrier, String sourceId, String itemId, String warehouseId) {
        TenantContext.set(tenant);
        try {
            barrier.await(10, TimeUnit.SECONDS);
            warehouseInventoryService.reserveStock("R-" + sourceId, "SALES_ORDER", sourceId, itemId, warehouseId, new BigDecimal("7"));
            return true;
        } catch (Exception ex) {
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    private Branch createBranch(String suffix) {
        Company company = companyRepository.save(new Company("CO-" + suffix, "Test Company", null, null, true));
        return branchRepository.save(new Branch(company.getId(), "BR-" + suffix, "Test Branch", null, true));
    }

    private void postingProfile(String event, String debitAccountId, String creditAccountId, LocalDate effectiveFrom) {
        PostingProfile profile = postingProfileRepository.save(new PostingProfile("O2C-" + event, event, effectiveFrom, null));
        postingProfileLineRepository.save(new PostingProfileLine(profile.getId(), 1, "DEBIT", "FIXED", debitAccountId, "AMOUNT"));
        postingProfileLineRepository.save(new PostingProfileLine(profile.getId(), 2, "CREDIT", "FIXED", creditAccountId, "AMOUNT"));
    }

    private void assertPostedSource(String type, String sourceId, String debitAccountId, String creditAccountId, String amount) {
        var metadata = journalSourceMetadataRepository.findAll().stream()
                .filter(row -> type.equals(row.getSourceDocumentType()) && sourceId.equals(row.getSourceDocumentId()))
                .toList();
        assertThat(metadata).singleElement().satisfies(row -> {
            JournalEntry journal = journalEntryRepository.findById(row.getJournalId()).orElseThrow();
            assertThat(journal.getStatus()).isEqualTo(JournalEntry.Status.POSTED);
            assertThat(journalEntryLineRepository.findByJournalEntryId(journal.getId()))
                    .anySatisfy(line -> {
                        assertThat(line.getAccountId()).isEqualTo(debitAccountId);
                        assertThat(line.getDebit()).isEqualByComparingTo(amount);
                    })
                    .anySatisfy(line -> {
                        assertThat(line.getAccountId()).isEqualTo(creditAccountId);
                        assertThat(line.getCredit()).isEqualByComparingTo(amount);
                    });
        });
    }
}
