package com.bemo.hr.operations.application;

import com.bemo.hr.operations.InventoryItem;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.StockMovement;
import com.bemo.hr.operations.StockMovementRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryAnalyticsService {

    private final InventoryItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Value
    @Builder
    public static class StockAgingItem {
        String itemId;
        String itemCode;
        String itemName;
        String unitCode;
        BigDecimal onHandQuantity;
        Long lastMovementDateMs;
        long inactiveDays;
        String agingBucket; // BUCKET_0_30, BUCKET_31_60, BUCKET_61_90, BUCKET_90_PLUS
        String trackingType;
        String barcode;
    }

    @Value
    @Builder
    public static class StockAgingSummary {
        BigDecimal totalOnHandItems;
        BigDecimal bucket0To30Qty;
        BigDecimal bucket31To60Qty;
        BigDecimal bucket61To90Qty;
        BigDecimal bucket90PlusQty;
        List<StockAgingItem> items;
    }

    @Value
    @Builder
    public static class DeadStockItem {
        String itemId;
        String itemCode;
        String itemName;
        String unitCode;
        BigDecimal onHandQuantity;
        Long lastMovementDateMs;
        long inactiveDays;
        boolean flaggedDeadStock;
    }

    @Value
    @Builder
    public static class ReorderAlertItem {
        String itemId;
        String itemCode;
        String itemName;
        String unitCode;
        BigDecimal onHandQuantity;
        BigDecimal reorderPoint;
        BigDecimal reorderQuantity;
        BigDecimal shortageQuantity;
        BigDecimal suggestedOrderQuantity;
        String urgency; // CRITICAL, WARNING, NOTICE
    }

    @Value
    @Builder
    public static class ProjectMaterialLine {
        String movementId;
        String itemId;
        String itemCode;
        String itemName;
        String unitCode;
        String operationType;
        BigDecimal quantityDelta;
        String projectId;
        String wbsNodeId;
        String costCodeId;
        String lotNumber;
        String serialNumber;
        String expiryDate;
        String binId;
        String referenceCode;
        String voucherNo;
        String createdBy;
        Long occurredAtMs;
    }

    @Value
    @Builder
    public static class BarcodeLookupResult {
        String itemId;
        String itemCode;
        String itemName;
        String itemType;
        String unitCode;
        String barcode;
        String barcodeAliases;
        String trackingType;
        Integer shelfLifeDays;
        BigDecimal onHandQuantity;
        BigDecimal reorderPoint;
        BigDecimal reorderQuantity;
        boolean active;
    }

    @Transactional(readOnly = true)
    public StockAgingSummary getStockAgingSummary() {
        List<InventoryItem> items = itemRepository.findAllByOrderByNameAsc();
        List<StockAgingItem> agingItems = new ArrayList<>();

        BigDecimal bucket0To30 = BigDecimal.ZERO;
        BigDecimal bucket31To60 = BigDecimal.ZERO;
        BigDecimal bucket61To90 = BigDecimal.ZERO;
        BigDecimal bucket90Plus = BigDecimal.ZERO;
        BigDecimal totalOnHand = BigDecimal.ZERO;

        Instant now = Instant.now();

        for (InventoryItem item : items) {
            BigDecimal onHand = stockMovementRepository.balance(item.getId());
            if (onHand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<StockMovement> lastMovement = stockMovementRepository.findFirstByItemIdOrderByOccurredAtDesc(item.getId());
            Instant lastOccurred = lastMovement.map(StockMovement::getOccurredAt)
                    .orElseGet(() -> item.getCreatedAt() != null ? item.getCreatedAt() : now);
            long inactiveDays = Math.max(0, ChronoUnit.DAYS.between(lastOccurred, now));

            String bucket;
            if (inactiveDays <= 30) {
                bucket = "BUCKET_0_30";
                bucket0To30 = bucket0To30.add(onHand);
            } else if (inactiveDays <= 60) {
                bucket = "BUCKET_31_60";
                bucket31To60 = bucket31To60.add(onHand);
            } else if (inactiveDays <= 90) {
                bucket = "BUCKET_61_90";
                bucket61To90 = bucket61To90.add(onHand);
            } else {
                bucket = "BUCKET_90_PLUS";
                bucket90Plus = bucket90Plus.add(onHand);
            }

            totalOnHand = totalOnHand.add(onHand);

            agingItems.add(StockAgingItem.builder()
                    .itemId(item.getId())
                    .itemCode(item.getCode())
                    .itemName(item.getName())
                    .unitCode(item.getUnitCode())
                    .onHandQuantity(onHand)
                    .lastMovementDateMs(lastOccurred.toEpochMilli())
                    .inactiveDays(inactiveDays)
                    .agingBucket(bucket)
                    .trackingType(item.getTrackingType())
                    .barcode(item.getBarcode())
                    .build());
        }

        return StockAgingSummary.builder()
                .totalOnHandItems(totalOnHand)
                .bucket0To30Qty(bucket0To30)
                .bucket31To60Qty(bucket31To60)
                .bucket61To90Qty(bucket61To90)
                .bucket90PlusQty(bucket90Plus)
                .items(agingItems)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DeadStockItem> getDeadStockItems(int thresholdDays) {
        List<InventoryItem> items = itemRepository.findAllByOrderByNameAsc();
        List<DeadStockItem> result = new ArrayList<>();
        Instant now = Instant.now();

        for (InventoryItem item : items) {
            BigDecimal onHand = stockMovementRepository.balance(item.getId());
            if (onHand.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Optional<StockMovement> lastMovement = stockMovementRepository.findFirstByItemIdOrderByOccurredAtDesc(item.getId());
            Instant lastOccurred = lastMovement.map(StockMovement::getOccurredAt)
                    .orElseGet(() -> item.getCreatedAt() != null ? item.getCreatedAt() : now);
            long inactiveDays = Math.max(0, ChronoUnit.DAYS.between(lastOccurred, now));

            if (item.isDeadStock() || inactiveDays >= thresholdDays) {
                result.add(DeadStockItem.builder()
                        .itemId(item.getId())
                        .itemCode(item.getCode())
                        .itemName(item.getName())
                        .unitCode(item.getUnitCode())
                        .onHandQuantity(onHand)
                        .lastMovementDateMs(lastOccurred.toEpochMilli())
                        .inactiveDays(inactiveDays)
                        .flaggedDeadStock(item.isDeadStock())
                        .build());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReorderAlertItem> getReorderAlerts() {
        List<InventoryItem> items = itemRepository.findAllByOrderByNameAsc();
        List<ReorderAlertItem> alerts = new ArrayList<>();

        for (InventoryItem item : items) {
            if (!item.isActive() || item.getReorderPoint().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal onHand = stockMovementRepository.balance(item.getId());
            if (onHand.compareTo(item.getReorderPoint()) <= 0) {
                BigDecimal shortage = item.getReorderPoint().subtract(onHand).max(BigDecimal.ZERO);
                BigDecimal suggested = shortage.add(item.getReorderQuantity());

                String urgency;
                if (onHand.compareTo(BigDecimal.ZERO) <= 0) {
                    urgency = "CRITICAL";
                } else if (onHand.compareTo(item.getReorderPoint().multiply(BigDecimal.valueOf(0.5))) <= 0) {
                    urgency = "WARNING";
                } else {
                    urgency = "NOTICE";
                }

                alerts.add(ReorderAlertItem.builder()
                        .itemId(item.getId())
                        .itemCode(item.getCode())
                        .itemName(item.getName())
                        .unitCode(item.getUnitCode())
                        .onHandQuantity(onHand)
                        .reorderPoint(item.getReorderPoint())
                        .reorderQuantity(item.getReorderQuantity())
                        .shortageQuantity(shortage)
                        .suggestedOrderQuantity(suggested)
                        .urgency(urgency)
                        .build());
            }
        }
        return alerts;
    }

    @Transactional(readOnly = true)
    public List<ProjectMaterialLine> getProjectMaterials(String projectId) {
        List<StockMovement> movements = stockMovementRepository.findByProjectIdOrderByOccurredAtDesc(projectId);
        List<ProjectMaterialLine> lines = new ArrayList<>();

        for (StockMovement m : movements) {
            Optional<InventoryItem> itemOpt = itemRepository.findById(m.getItemId());
            lines.add(ProjectMaterialLine.builder()
                    .movementId(m.getId())
                    .itemId(m.getItemId())
                    .itemCode(itemOpt.map(InventoryItem::getCode).orElse("—"))
                    .itemName(itemOpt.map(InventoryItem::getName).orElse("—"))
                    .unitCode(itemOpt.map(InventoryItem::getUnitCode).orElse("—"))
                    .operationType(m.getOperationType())
                    .quantityDelta(m.getQuantityDelta())
                    .projectId(m.getProjectId())
                    .wbsNodeId(m.getWbsNodeId())
                    .costCodeId(m.getCostCodeId())
                    .lotNumber(m.getLotNumber())
                    .serialNumber(m.getSerialNumber())
                    .expiryDate(m.getExpiryDate())
                    .binId(m.getBinId())
                    .referenceCode(m.getReferenceCode())
                    .voucherNo(m.getVoucherNo())
                    .createdBy(m.getCreatedBy())
                    .occurredAtMs(m.getOccurredAt().toEpochMilli())
                    .build());
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public Optional<BarcodeLookupResult> lookupBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return Optional.empty();
        }
        String clean = barcode.strip();
        List<InventoryItem> matched = itemRepository.findByBarcodeOrAlias(clean);
        if (matched.isEmpty()) {
            return Optional.empty();
        }
        InventoryItem item = matched.get(0);
        BigDecimal onHand = stockMovementRepository.balance(item.getId());

        return Optional.of(BarcodeLookupResult.builder()
                .itemId(item.getId())
                .itemCode(item.getCode())
                .itemName(item.getName())
                .itemType(item.getItemType())
                .unitCode(item.getUnitCode())
                .barcode(item.getBarcode())
                .barcodeAliases(item.getBarcodeAliases())
                .trackingType(item.getTrackingType())
                .shelfLifeDays(item.getShelfLifeDays())
                .onHandQuantity(onHand)
                .reorderPoint(item.getReorderPoint())
                .reorderQuantity(item.getReorderQuantity())
                .active(item.isActive())
                .build());
    }
}
