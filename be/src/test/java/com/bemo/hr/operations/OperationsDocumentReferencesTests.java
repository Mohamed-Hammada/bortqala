package com.bemo.hr.operations;

import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OperationsDocumentReferencesTests {

    private final OperationsService service;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final TenantApplicationRepository tenantApplicationRepository;

    private final List<String> createdItems = new ArrayList<>();
    private final List<String> createdParties = new ArrayList<>();
    private final List<String> createdMovements = new ArrayList<>();
    private final List<String> createdLedgerEntries = new ArrayList<>();
    private final List<String> createdApps = new ArrayList<>();

    @Autowired
    OperationsDocumentReferencesTests(OperationsService service,
                                      InventoryItemRepository inventoryItemRepository,
                                      StockMovementRepository stockMovementRepository,
                                      PartnerLedgerEntryRepository partnerLedgerEntryRepository,
                                      BusinessPartyRepository businessPartyRepository,
                                      TenantApplicationRepository tenantApplicationRepository) {
        this.service = service;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.partnerLedgerEntryRepository = partnerLedgerEntryRepository;
        this.businessPartyRepository = businessPartyRepository;
        this.tenantApplicationRepository = tenantApplicationRepository;
    }

    @AfterEach
    void cleanup() {
        try {
            if (createdApps.isEmpty()) return;
            stockMovementRepository.deleteAllById(createdMovements);
            partnerLedgerEntryRepository.deleteAllById(createdLedgerEntries);
            businessPartyRepository.deleteAllById(createdParties);
            inventoryItemRepository.deleteAllById(createdItems);
            tenantApplicationRepository.deleteAllById(createdApps);
        } finally {
            TenantContext.clear();
        }
    }

    private TenantApplication app() {
        var created = tenantApplicationRepository.save(
                new TenantApplication("APP-REF-" + UUID.randomUUID().toString().substring(0, 6),
                        "APP-REF-" + UUID.randomUUID().toString().substring(0, 6)));
        createdApps.add(created.getId());
        TenantContext.set(created.getId());
        return created;
    }

    private String party() {
        var saved = businessPartyRepository.save(new BusinessParty(
                "SUP-" + UUID.randomUUID().toString().substring(0, 4), "Supplier Co", null, "SUPPLIER",
                null, null, null, null, null, true, "DIRECT", null, null, null, "EGP", "BY_INVOICE", "NET30", null, null));
        createdParties.add(saved.getId());
        return saved.getId();
    }

    private String item() {
        var saved = inventoryItemRepository.save(new InventoryItem(
                "ITM-" + UUID.randomUUID().toString().substring(0, 4), "Raw Material", "RAW_MATERIAL", "KG"));
        createdItems.add(saved.getId());
        return saved.getId();
    }

    private OperationsApi.TransactionRequest request(String operationType, String partyId, String receiptNo,
                                                     String purchaseOrderNo, String deliveryNoteNo, String invoiceNo,
                                                     String voucherNo) {
        return new OperationsApi.TransactionRequest(
                item(), partyId, operationType, new BigDecimal("5"), new BigDecimal("0"), null,
                null, null, null, null, purchaseOrderNo, receiptNo, deliveryNoteNo, invoiceNo,
                voucherNo, null, null, null, null, null, Instant.now());
    }

    @Test
    void supplyReceiptRequiresReceiptAndPurchaseOrderNumbers() {
        app();
        assertThatThrownBy(() -> service.recordTransaction(
                request("SUPPLY_RECEIPT", null, null, null, null, null, null), "qa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("receipt document number")
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("OPS_MOVEMENT_RECEIPT_REQUIRED"));

        assertThatThrownBy(() -> service.recordTransaction(
                request("SUPPLY_RECEIPT", null, "REC-1", null, null, null, null), "qa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("purchase-order number")
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("OPS_MOVEMENT_PURCHASE_ORDER_REQUIRED"));
    }

    @Test
    void saleDeliveryRequiresDeliveryNoteNumber() {
        app();
        assertThatThrownBy(() -> service.recordTransaction(
                request("EXPORT_SALE", null, null, null, null, null, null), "qa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("delivery-note number")
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("OPS_MOVEMENT_DELIVERY_NOTE_REQUIRED"));
    }

    @Test
    void adjustmentRequiresVoucherNumber() {
        app();
        assertThatThrownBy(() -> service.recordTransaction(
                request("ADJUSTMENT", null, null, null, null, null, null), "qa"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("voucher number")
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("OPS_MOVEMENT_VOUCHER_REQUIRED"));
    }

    @Test
    void invoiceNumberRequiresPartyAndRejectsDuplicatesPerSupplier() {
        app();
        var supplierId = party();

        assertThatThrownBy(() -> service.recordTransaction(
                request("SUPPLY_RECEIPT", null, "REC-1", "PO-1", null, "INV-DUP", null), "qa"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("OPS_MOVEMENT_INVOICE_PARTY_REQUIRED"));

        var seeded = stockMovementRepository.save(new StockMovement(
                item(), supplierId, "SUPPLY_RECEIPT", new BigDecimal("1"), null, null, null, Instant.now(), "qa"));
        seeded.assignDocument("GOODS_RECEIPT", null);
        seeded.assignReferences("PO-1", "REC-1", null, "INV-DUP", null, null, null, null, null, null);
        stockMovementRepository.save(seeded);
        createdMovements.add(seeded.getId());

        assertThatThrownBy(() -> service.recordTransaction(
                request("SUPPLY_RECEIPT", supplierId, "REC-2", "PO-2", null, "INV-DUP", null), "qa"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(e -> assertThat(((BusinessRuleException) e).getCode()).isEqualTo("OPS_MOVEMENT_INVOICE_DUPLICATE"));
    }

    @Test
    void validSupplyReceiptStoresSeparateReferencesAndDerivesDocumentType() {
        app();
        var supplierId = party();
        var itemId = item();

        var snapshot = service.recordTransaction(new OperationsApi.TransactionRequest(
                itemId, supplierId, "SUPPLY_RECEIPT", new BigDecimal("5"), new BigDecimal("0"), null,
                null, "picked up from dock", null, null, "PO-100", "REC-100", "DN-100", "INV-100",
                null, "EXT-REF", "Main Warehouse", "receipt.pdf", "application/pdf", 2048L, Instant.now()), "qa");
        createdMovements.add(snapshot.movements().get(0).id());

        var saved = snapshot.movements().get(0);
        assertThat(saved.documentType()).isEqualTo("GOODS_RECEIPT");
        assertThat(saved.purchaseOrderNo()).isEqualTo("PO-100");
        assertThat(saved.receiptNo()).isEqualTo("REC-100");
        assertThat(saved.deliveryNoteNo()).isEqualTo("DN-100");
        assertThat(saved.invoiceNo()).isEqualTo("INV-100");
        assertThat(saved.externalRef()).isEqualTo("EXT-REF");
        assertThat(saved.warehouse()).isEqualTo("Main Warehouse");
        assertThat(saved.attachmentName()).isEqualTo("receipt.pdf");
        assertThat(saved.attachmentContentType()).isEqualTo("application/pdf");
        assertThat(saved.attachmentSize()).isEqualTo(2048L);
    }

    @Test
    void financialOnlyMovementSkipsReferenceRequirements() {
        app();
        var supplierId = party();
        var itemId = item();

        var snapshot = service.recordTransaction(new OperationsApi.TransactionRequest(
                itemId, supplierId, "PAYMENT", new BigDecimal("0"), new BigDecimal("100"), null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, Instant.now()), "qa");
        createdLedgerEntries.add(snapshot.ledgerEntries().get(0).id());

        assertThat(snapshot.movements()).isEmpty();
        assertThat(snapshot.ledgerEntries()).hasSize(1);
        assertThat(snapshot.ledgerEntries().get(0).entryType()).isEqualTo("PAYMENT");
        assertThat(snapshot.ledgerEntries().get(0).amountDelta()).isEqualByComparingTo(new BigDecimal("100"));
    }
}
