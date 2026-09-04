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
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.AppUser;
import com.bemo.hr.shared.security.AppUserRepository;
import com.bemo.hr.trade.fieldsales.api.FieldSalesApi;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesDocumentType;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesOfflineTransaction;
import com.bemo.hr.trade.fieldsales.domain.FieldSalesSyncStatus;
import com.bemo.hr.trade.fieldsales.infrastructure.FieldSalesOfflineTransactionRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldSalesService {

    private final FieldSalesOfflineTransactionRepository transactionRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseInventoryService warehouseInventoryService;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public FieldSalesApi.OfflineBundleResponse getOfflineBundle(String salesRepUserId) {
        log.info("Generating offline field sales bundle for sales rep {}", salesRepUserId);

        String salesRepName = appUserRepository.findByUsernameIgnoreCase(salesRepUserId)
                .map(AppUser::getDisplayName)
                .orElse(salesRepUserId);

        List<BusinessParty> parties = businessPartyRepository.findAll();
        List<FieldSalesApi.CustomerSummary> customers = parties.stream()
                .filter(BusinessParty::isActive)
                .map(party -> {
                    BigDecimal balance = calculateCustomerBalance(party.getId());
                    return new FieldSalesApi.CustomerSummary(
                            party.getId(),
                            party.getCode(),
                            party.getName(),
                            party.getPhone(),
                            party.getAddress(),
                            party.getTaxId(),
                            party.getCreditLimit() != null ? party.getCreditLimit() : BigDecimal.ZERO,
                            balance,
                            party.isCreditHold(),
                            party.getPaymentTermsDays() != null ? party.getPaymentTermsDays() : 30
                    );
                })
                .toList();

        List<InventoryItem> items = inventoryItemRepository.findAllByOrderByNameAsc();
        List<FieldSalesApi.ProductSummary> products = items.stream()
                .filter(InventoryItem::isActive)
                .map(item -> {
                    BigDecimal price = item.getReorderQuantity() != null && item.getReorderQuantity().compareTo(BigDecimal.ZERO) > 0
                            ? item.getReorderQuantity()
                            : BigDecimal.valueOf(100.00);
                    return new FieldSalesApi.ProductSummary(
                            item.getId(),
                            item.getCode(),
                            item.getName(),
                            item.getUnitCode() != null ? item.getUnitCode() : "PCS",
                            price,
                            BigDecimal.valueOf(14.00),
                            BigDecimal.valueOf(500.00)
                    );
                })
                .toList();

        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<FieldSalesApi.WarehouseSummary> warehouseSummaries = warehouses.stream()
                .filter(Warehouse::isActive)
                .map(w -> new FieldSalesApi.WarehouseSummary(w.getId(), w.getCode(), w.getName()))
                .toList();

        return new FieldSalesApi.OfflineBundleResponse(
                customers,
                products,
                warehouseSummaries,
                salesRepUserId,
                salesRepName,
                System.currentTimeMillis()
        );
    }

    @Transactional
    public FieldSalesApi.SyncBatchResponse syncBatch(FieldSalesApi.SyncBatchRequest request, String salesRepUserId, String actor) {
        log.info("Processing field sales sync batch with {} items for user {}", request.transactions().size(), salesRepUserId);

        List<FieldSalesApi.SyncResultItem> results = new ArrayList<>();
        int syncedCount = 0;
        int conflictCount = 0;

        for (FieldSalesApi.SyncTransactionRequestItem tx : request.transactions()) {
            Optional<FieldSalesOfflineTransaction> existingOpt = transactionRepository.findByClientOfflineId(tx.clientOfflineId());
            if (existingOpt.isPresent()) {
                FieldSalesOfflineTransaction existing = existingOpt.get();
                log.info("Idempotent replay detected for offline ID {}", tx.clientOfflineId());
                results.add(new FieldSalesApi.SyncResultItem(
                        tx.clientOfflineId(),
                        existing.getServerDocumentId(),
                        existing.getServerDocumentNumber(),
                        existing.getStatus(),
                        existing.getConflictReason(),
                        "Transaction already synchronized (idempotent replay)"
                ));
                if (existing.getStatus() == FieldSalesSyncStatus.SYNCED) {
                    syncedCount++;
                } else {
                    conflictCount++;
                }
                continue;
            }

            Optional<BusinessParty> customerOpt = businessPartyRepository.findById(tx.customerId());
            if (customerOpt.isEmpty()) {
                FieldSalesOfflineTransaction conflictTx = recordConflict(tx, salesRepUserId, "FIELD_SALES_CUSTOMER_NOT_FOUND");
                results.add(new FieldSalesApi.SyncResultItem(
                        tx.clientOfflineId(),
                        conflictTx.getId(),
                        null,
                        FieldSalesSyncStatus.CONFLICT,
                        "FIELD_SALES_CUSTOMER_NOT_FOUND",
                        "Customer does not exist in master records"
                ));
                conflictCount++;
                continue;
            }

            BusinessParty customer = customerOpt.get();
            if (customer.isCreditHold()) {
                FieldSalesOfflineTransaction conflictTx = recordConflict(tx, salesRepUserId, "FIELD_SALES_CONFLICT_CREDIT_EXCEEDED");
                results.add(new FieldSalesApi.SyncResultItem(
                        tx.clientOfflineId(),
                        conflictTx.getId(),
                        null,
                        FieldSalesSyncStatus.CONFLICT,
                        "FIELD_SALES_CONFLICT_CREDIT_EXCEEDED",
                        "Customer account is on credit hold"
                ));
                conflictCount++;
                continue;
            }

            if (customer.getCreditLimit() != null && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0
                    && (tx.documentType() == FieldSalesDocumentType.INVOICE || tx.documentType() == FieldSalesDocumentType.ORDER)) {
                BigDecimal currentBalance = calculateCustomerBalance(customer.getId());
                BigDecimal projectedBalance = currentBalance.add(tx.totalAmount());
                if (projectedBalance.compareTo(customer.getCreditLimit()) > 0) {
                    FieldSalesOfflineTransaction conflictTx = recordConflict(tx, salesRepUserId, "FIELD_SALES_CONFLICT_CREDIT_EXCEEDED");
                    results.add(new FieldSalesApi.SyncResultItem(
                        tx.clientOfflineId(),
                        conflictTx.getId(),
                        null,
                        FieldSalesSyncStatus.CONFLICT,
                        "FIELD_SALES_CONFLICT_CREDIT_EXCEEDED",
                        "Customer credit limit exceeded: current " + currentBalance + ", limit " + customer.getCreditLimit()
                    ));
                    conflictCount++;
                    continue;
                }
            }

            String serverDocNo = generateServerDocNumber(tx.documentType());
            String serverDocId = UUID.randomUUID().toString();
            Instant occurredAt = tx.clientCreatedAt() > 0 ? Instant.ofEpochMilli(tx.clientCreatedAt()) : Instant.now();

            processBusinessDocument(tx, serverDocNo, customer, occurredAt, actor);

            FieldSalesOfflineTransaction savedTx = new FieldSalesOfflineTransaction(
                    tx.clientOfflineId(),
                    tx.documentType(),
                    tx.offlineDocumentNumber(),
                    tx.customerId(),
                    tx.customerName() != null ? tx.customerName() : customer.getName(),
                    salesRepUserId,
                    tx.totalAmount(),
                    FieldSalesSyncStatus.SYNCED,
                    null,
                    tx.customerSignaturePng(),
                    tx.customerConfirmationName(),
                    tx.gpsCoordinates(),
                    serializePayload(tx),
                    occurredAt
            );
            savedTx.markSynced(serverDocId, serverDocNo);
            transactionRepository.save(savedTx);

            auditService.record("FIELD_SALES_SYNC", "FieldSalesOfflineTransaction", savedTx.getId(), actor,
                    "Synchronized field sales " + tx.documentType() + " document " + serverDocNo + " for customer " + customer.getName(), null);

            results.add(new FieldSalesApi.SyncResultItem(
                    tx.clientOfflineId(),
                    serverDocId,
                    serverDocNo,
                    FieldSalesSyncStatus.SYNCED,
                    null,
                    "Document synchronized successfully"
            ));
            syncedCount++;
        }

        return new FieldSalesApi.SyncBatchResponse(request.transactions().size(), syncedCount, conflictCount, results);
    }

    @Transactional(readOnly = true)
    public List<FieldSalesApi.OfflineTransactionRecordResponse> getSyncHistory(String salesRepUserId) {
        return transactionRepository.findAllBySalesRepUserIdOrderByCreatedAtDesc(salesRepUserId).stream()
                .map(tx -> new FieldSalesApi.OfflineTransactionRecordResponse(
                        tx.getId(),
                        tx.getClientOfflineId(),
                        tx.getDocumentType(),
                        tx.getOfflineDocumentNumber(),
                        tx.getServerDocumentId(),
                        tx.getServerDocumentNumber(),
                        tx.getCustomerId(),
                        tx.getCustomerName(),
                        tx.getSalesRepUserId(),
                        tx.getTotalAmount(),
                        tx.getStatus(),
                        tx.getConflictReason(),
                        tx.getCustomerConfirmationName(),
                        tx.getGpsCoordinates(),
                        tx.getClientCreatedAt() != null ? tx.getClientCreatedAt().toEpochMilli() : 0L,
                        tx.getSyncedAt() != null ? tx.getSyncedAt().toEpochMilli() : 0L
                ))
                .toList();
    }

    private void processBusinessDocument(FieldSalesApi.SyncTransactionRequestItem tx, String serverDocNo,
                                         BusinessParty customer, Instant occurredAt, String actor) {
        if (tx.documentType() == FieldSalesDocumentType.INVOICE) {
            PartnerLedgerEntry ledgerEntry = new PartnerLedgerEntry(
                    customer.getId(),
                    "SALES_INVOICE",
                    tx.totalAmount(),
                    serverDocNo,
                    "Field Sales Offline Invoice: " + tx.offlineDocumentNumber(),
                    occurredAt,
                    actor
            );
            partnerLedgerEntryRepository.save(ledgerEntry);

            if (tx.lines() != null) {
                for (FieldSalesApi.SyncLineItem line : tx.lines()) {
                    StockMovement movement = new StockMovement(
                            line.itemId(),
                            customer.getId(),
                            "FIELD_SALE",
                            line.quantity().negate(),
                            null,
                            serverDocNo,
                            "Direct Van Delivery: " + serverDocNo,
                            occurredAt,
                            actor
                    );
                    movement.assignDocument("DELIVERY_NOTE", "Field sales offline direct delivery");
                    stockMovementRepository.save(movement);
                }
            }
        } else if (tx.documentType() == FieldSalesDocumentType.RECEIPT) {
            PartnerLedgerEntry ledgerEntry = new PartnerLedgerEntry(
                    customer.getId(),
                    "CUSTOMER_PAYMENT",
                    tx.totalAmount().negate(),
                    serverDocNo,
                    "Field Sales Payment Receipt: " + tx.offlineDocumentNumber() + " (" + (tx.paymentMethod() != null ? tx.paymentMethod() : "CASH") + ")",
                    occurredAt,
                    actor
            );
            partnerLedgerEntryRepository.save(ledgerEntry);
        } else if (tx.documentType() == FieldSalesDocumentType.RETURN) {
            PartnerLedgerEntry ledgerEntry = new PartnerLedgerEntry(
                    customer.getId(),
                    "SALES_RETURN",
                    tx.totalAmount().negate(),
                    serverDocNo,
                    "Field Sales Customer Return: " + tx.offlineDocumentNumber() + " - " + (tx.returnReason() != null ? tx.returnReason() : "Defective/Rejection"),
                    occurredAt,
                    actor
            );
            partnerLedgerEntryRepository.save(ledgerEntry);

            if (tx.lines() != null) {
                for (FieldSalesApi.SyncLineItem line : tx.lines()) {
                    StockMovement movement = new StockMovement(
                            line.itemId(),
                            customer.getId(),
                            "FIELD_SALE_RETURN",
                            line.quantity(),
                            null,
                            serverDocNo,
                            "Van Restock Return: " + serverDocNo,
                            occurredAt,
                            actor
                    );
                    movement.assignDocument("RETURN_NOTE", "Field sales offline customer return restock");
                    stockMovementRepository.save(movement);
                }
            }
        }
    }

    private FieldSalesOfflineTransaction recordConflict(FieldSalesApi.SyncTransactionRequestItem tx, String salesRepUserId, String reason) {
        FieldSalesOfflineTransaction entity = new FieldSalesOfflineTransaction(
                tx.clientOfflineId(),
                tx.documentType(),
                tx.offlineDocumentNumber(),
                tx.customerId(),
                tx.customerName(),
                salesRepUserId,
                tx.totalAmount(),
                FieldSalesSyncStatus.CONFLICT,
                reason,
                tx.customerSignaturePng(),
                tx.customerConfirmationName(),
                tx.gpsCoordinates(),
                serializePayload(tx),
                tx.clientCreatedAt() > 0 ? Instant.ofEpochMilli(tx.clientCreatedAt()) : Instant.now()
        );
        return transactionRepository.save(entity);
    }

    private BigDecimal calculateCustomerBalance(String customerId) {
        List<PartnerLedgerEntry> entries = partnerLedgerEntryRepository.findByPartyIdOrderByOccurredAtDesc(customerId);
        BigDecimal balance = BigDecimal.ZERO;
        for (PartnerLedgerEntry entry : entries) {
            if (entry.getAmountDelta() != null) {
                balance = balance.add(entry.getAmountDelta());
            }
        }
        return balance;
    }

    private String generateServerDocNumber(FieldSalesDocumentType documentType) {
        String prefix = switch (documentType) {
            case INVOICE -> "FS-INV";
            case ORDER -> "FS-ORD";
            case RECEIPT -> "FS-REC";
            case RETURN -> "FS-RET";
            case QUOTATION -> "FS-QTN";
        };
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String uuidSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + dateStr + "-" + uuidSuffix;
    }

    private String serializePayload(FieldSalesApi.SyncTransactionRequestItem tx) {
        try {
            return objectMapper.writeValueAsString(tx);
        } catch (Exception e) {
            log.warn("Failed to serialize field sales payload for offline ID {}", tx.clientOfflineId(), e);
            return "{}";
        }
    }
}
