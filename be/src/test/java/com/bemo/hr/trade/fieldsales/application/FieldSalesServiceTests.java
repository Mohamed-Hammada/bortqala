package com.bemo.hr.trade.fieldsales.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.PartnerLedgerEntry;
import com.bemo.hr.operations.PartnerLedgerEntryRepository;
import com.bemo.hr.operations.StockMovement;
import com.bemo.hr.operations.StockMovementRepository;
import com.bemo.hr.operations.application.WarehouseInventoryService;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.trade.fieldsales.api.FieldSalesApi;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesDocumentType;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesOfflineTransaction;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesSyncStatus;
import com.bemo.hr.trade.fieldsales.infrastructure.FieldSalesOfflineTransactionRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FieldSalesServiceTests {

    @Mock
    FieldSalesOfflineTransactionRepository transactionRepository;
    @Mock
    BusinessPartyRepository businessPartyRepository;
    @Mock
    InventoryItemRepository inventoryItemRepository;
    @Mock
    WarehouseRepository warehouseRepository;
    @Mock
    PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    @Mock
    StockMovementRepository stockMovementRepository;
    @Mock
    WarehouseInventoryService warehouseInventoryService;
    @Mock
    AppUserRepository appUserRepository;
    @Mock
    AuditService auditService;

    ObjectMapper objectMapper = new ObjectMapper();
    FieldSalesService service;

    BusinessParty customer;
    InventoryItem item;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new FieldSalesService(
                transactionRepository,
                businessPartyRepository,
                inventoryItemRepository,
                warehouseRepository,
                partnerLedgerEntryRepository,
                stockMovementRepository,
                warehouseInventoryService,
                appUserRepository,
                auditService,
                objectMapper
        );

        customer = new BusinessParty("CUST-001", "Al-Ahram Trading", "Al-Ahram Trading", "CUSTOMER",
                "Ahmed Mohsen", "01012345678", "ahmed@alahram.eg", "Cairo, Egypt",
                "Key distributor", true, "DIRECT", null,
                "2026-01-01", null, "EGP", "STANDARD", "NET30",
                "123-456-789", "EG1234567890");
        ReflectionTestUtils.setField(customer, "id", "cust-1");
        ReflectionTestUtils.setField(customer, "creditLimit", BigDecimal.valueOf(50000.00));
        ReflectionTestUtils.setField(customer, "creditHold", false);

        item = new InventoryItem("ITEM-001", "Delta Premium Juice 1L", "FINISHED_GOOD", "PCS");
        ReflectionTestUtils.setField(item, "id", "item-1");
        ReflectionTestUtils.setField(item, "active", true);
        ReflectionTestUtils.setField(item, "reorderQuantity", BigDecimal.valueOf(25.50));

        warehouse = new Warehouse("branch-1", "WH-VAN-01", "Van #1 Mobile Stock", "Cairo Route 1", true);
        ReflectionTestUtils.setField(warehouse, "id", "wh-1");
    }

    @Test
    void getOfflineBundleCompilesCustomersItemsAndWarehouses() {
        when(appUserRepository.findByUsernameIgnoreCase("sales_rep_1")).thenReturn(Optional.empty());
        when(businessPartyRepository.findAll()).thenReturn(List.of(customer));
        when(inventoryItemRepository.findAllByOrderByNameAsc()).thenReturn(List.of(item));
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));
        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc("cust-1")).thenReturn(List.of(
                new PartnerLedgerEntry("cust-1", "SALES_INVOICE", BigDecimal.valueOf(1500.00), "INV-001", "Initial", Instant.now(), "system")
        ));

        FieldSalesApi.OfflineBundleResponse bundle = service.getOfflineBundle("sales_rep_1");

        assertThat(bundle).isNotNull();
        assertThat(bundle.customers()).hasSize(1);
        assertThat(bundle.customers().get(0).code()).isEqualTo("CUST-001");
        assertThat(bundle.customers().get(0).currentBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500.00));
        assertThat(bundle.products()).hasSize(1);
        assertThat(bundle.products().get(0).itemCode()).isEqualTo("ITEM-001");
        assertThat(bundle.products().get(0).basePrice()).isEqualByComparingTo(BigDecimal.valueOf(25.50));
        assertThat(bundle.warehouses()).hasSize(1);
        assertThat(bundle.warehouses().get(0).warehouseCode()).isEqualTo("WH-VAN-01");
    }

    @Test
    void syncBatchProcessesInvoicesAndReceiptsSuccessfully() {
        when(transactionRepository.findByClientOfflineId(any())).thenReturn(Optional.empty());
        when(businessPartyRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc("cust-1")).thenReturn(List.of());

        FieldSalesApi.SyncLineItem line = new FieldSalesApi.SyncLineItem(
                "item-1", "ITEM-001", "Delta Juice 1L", "PCS",
                BigDecimal.valueOf(10), BigDecimal.valueOf(25.00), BigDecimal.ZERO,
                BigDecimal.valueOf(35.00), BigDecimal.valueOf(285.00)
        );

        FieldSalesApi.SyncTransactionRequestItem invoiceTx = new FieldSalesApi.SyncTransactionRequestItem(
                "off-uuid-1", FieldSalesDocumentType.INVOICE, "OFF-INV-2026-0001", "cust-1",
                "Al-Ahram Trading", "wh-1", BigDecimal.valueOf(250.00), BigDecimal.ZERO,
                BigDecimal.valueOf(35.00), BigDecimal.valueOf(285.00), List.of(line),
                "CASH", null, null, "data:image/png;base64,iVBORw0KGgo...", "Ahmed Mohsen",
                "30.0444,31.2357", "Direct sale", System.currentTimeMillis()
        );

        FieldSalesApi.SyncTransactionRequestItem receiptTx = new FieldSalesApi.SyncTransactionRequestItem(
                "off-uuid-2", FieldSalesDocumentType.RECEIPT, "OFF-REC-2026-0001", "cust-1",
                "Al-Ahram Trading", "wh-1", BigDecimal.valueOf(200.00), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(200.00), List.of(),
                "CASH", "OFF-INV-2026-0001", null, null, null,
                "30.0444,31.2357", "Payment collection", System.currentTimeMillis()
        );

        FieldSalesApi.SyncBatchRequest request = new FieldSalesApi.SyncBatchRequest(List.of(invoiceTx, receiptTx));

        FieldSalesApi.SyncBatchResponse response = service.syncBatch(request, "rep-user-1", "rep-user-1");

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.syncedCount()).isEqualTo(2);
        assertThat(response.conflictCount()).isEqualTo(0);

        verify(partnerLedgerEntryRepository, times(2)).save(any(PartnerLedgerEntry.class));
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
        verify(transactionRepository, times(2)).save(any(FieldSalesOfflineTransaction.class));
        verify(auditService, times(2)).record(eq("FIELD_SALES_SYNC"), eq("FieldSalesOfflineTransaction"), any(), eq("rep-user-1"), any(), any());
    }

    @Test
    void syncBatchReturnsIdempotentReplayForAlreadySyncedTx() {
        FieldSalesOfflineTransaction existing = new FieldSalesOfflineTransaction(
                "off-uuid-existing", FieldSalesDocumentType.INVOICE, "OFF-INV-0001", "cust-1",
                "Al-Ahram Trading", "rep-user-1", BigDecimal.valueOf(150.00), FieldSalesSyncStatus.SYNCED,
                null, null, null, null, "{}", Instant.now()
        );
        existing.markSynced("server-doc-1", "FS-INV-20260904-ABC123");

        when(transactionRepository.findByClientOfflineId("off-uuid-existing")).thenReturn(Optional.of(existing));

        FieldSalesApi.SyncTransactionRequestItem requestItem = new FieldSalesApi.SyncTransactionRequestItem(
                "off-uuid-existing", FieldSalesDocumentType.INVOICE, "OFF-INV-0001", "cust-1",
                "Al-Ahram Trading", "wh-1", BigDecimal.valueOf(150.00), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(150.00), List.of(),
                "CASH", null, null, null, null, null, null, System.currentTimeMillis()
        );

        FieldSalesApi.SyncBatchResponse response = service.syncBatch(
                new FieldSalesApi.SyncBatchRequest(List.of(requestItem)), "rep-user-1", "rep-user-1");

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.syncedCount()).isEqualTo(1);
        assertThat(response.results().get(0).serverDocumentNumber()).isEqualTo("FS-INV-20260904-ABC123");
        verify(partnerLedgerEntryRepository, never()).save(any());
    }

    @Test
    void syncBatchDetectsCreditHoldConflict() {
        ReflectionTestUtils.setField(customer, "creditHold", true);

        when(transactionRepository.findByClientOfflineId("off-uuid-hold")).thenReturn(Optional.empty());
        when(businessPartyRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FieldSalesApi.SyncTransactionRequestItem requestItem = new FieldSalesApi.SyncTransactionRequestItem(
                "off-uuid-hold", FieldSalesDocumentType.INVOICE, "OFF-INV-9999", "cust-1",
                "Al-Ahram Trading", "wh-1", BigDecimal.valueOf(500.00), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(500.00), List.of(),
                "ON_ACCOUNT", null, null, null, null, null, null, System.currentTimeMillis()
        );

        FieldSalesApi.SyncBatchResponse response = service.syncBatch(
                new FieldSalesApi.SyncBatchRequest(List.of(requestItem)), "rep-user-1", "rep-user-1");

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.syncedCount()).isEqualTo(0);
        assertThat(response.conflictCount()).isEqualTo(1);
        assertThat(response.results().get(0).status()).isEqualTo(FieldSalesSyncStatus.CONFLICT);
        assertThat(response.results().get(0).conflictReason()).isEqualTo("FIELD_SALES_CONFLICT_CREDIT_EXCEEDED");
    }

    @Test
    void syncBatchDetectsCreditLimitExceededConflict() {
        ReflectionTestUtils.setField(customer, "creditLimit", BigDecimal.valueOf(1000.00));

        when(transactionRepository.findByClientOfflineId("off-uuid-limit")).thenReturn(Optional.empty());
        when(businessPartyRepository.findById("cust-1")).thenReturn(Optional.of(customer));
        when(partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc("cust-1")).thenReturn(List.of(
                new PartnerLedgerEntry("cust-1", "SALES_INVOICE", BigDecimal.valueOf(800.00), "INV-OLD", "Old", Instant.now(), "sys")
        ));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FieldSalesApi.SyncTransactionRequestItem requestItem = new FieldSalesApi.SyncTransactionRequestItem(
                "off-uuid-limit", FieldSalesDocumentType.INVOICE, "OFF-INV-OVER", "cust-1",
                "Al-Ahram Trading", "wh-1", BigDecimal.valueOf(300.00), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(300.00), List.of(),
                "ON_ACCOUNT", null, null, null, null, null, null, System.currentTimeMillis()
        );

        FieldSalesApi.SyncBatchResponse response = service.syncBatch(
                new FieldSalesApi.SyncBatchRequest(List.of(requestItem)), "rep-user-1", "rep-user-1");

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.syncedCount()).isEqualTo(0);
        assertThat(response.conflictCount()).isEqualTo(1);
        assertThat(response.results().get(0).status()).isEqualTo(FieldSalesSyncStatus.CONFLICT);
        assertThat(response.results().get(0).conflictReason()).isEqualTo("FIELD_SALES_CONFLICT_CREDIT_EXCEEDED");
    }
}
