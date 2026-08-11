package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.manufacturing.production.api.ManufacturingApi;
import com.bemo.hr.manufacturing.production.domain.BomHeader;
import com.bemo.hr.manufacturing.production.domain.BomLine;
import com.bemo.hr.manufacturing.production.domain.ProductionOrder;
import com.bemo.hr.manufacturing.production.domain.QualityInspection;
import com.bemo.hr.manufacturing.production.infrastructure.BomHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.BomLineRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.QualityInspectionRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ManufacturingService {

    private final BomHeaderRepository bomHeaderRepository;
    private final BomLineRepository bomLineRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final QualityInspectionRepository qualityInspectionRepository;
    private final OperationsService operationsService;
    private final AuditService auditService;
    private final BomSnapshotService bomSnapshotService;

    public ManufacturingService(BomHeaderRepository bomHeaderRepository,
                                BomLineRepository bomLineRepository,
                                ProductionOrderRepository productionOrderRepository,
                                QualityInspectionRepository qualityInspectionRepository,
                                OperationsService operationsService,
                                AuditService auditService,
                                BomSnapshotService bomSnapshotService) {
        this.bomHeaderRepository = bomHeaderRepository;
        this.bomLineRepository = bomLineRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.qualityInspectionRepository = qualityInspectionRepository;
        this.operationsService = operationsService;
        this.auditService = auditService;
        this.bomSnapshotService = bomSnapshotService;
    }

    // ─── BOM Management ─────────────────────────────────────────────

    public List<ManufacturingApi.BomResponse> listBoms() {
        return bomHeaderRepository.findAllByOrderByBomCodeAsc().stream().map(this::toBomResponse).toList();
    }

    @Transactional
    public ManufacturingApi.BomResponse createBom(ManufacturingApi.BomPayload payload) {
        if (bomHeaderRepository.existsByBomCodeIgnoreCase(payload.bomCode().strip())) {
            throw new BusinessRuleException("كود قائمة المواد مستخدم بالفعل.", "MFG_BOM_CODE_EXISTS", HttpStatus.CONFLICT);
        }
        LocalDate effFrom = payload.effectiveFrom() != null ? Instant.ofEpochMilli(payload.effectiveFrom()).atZone(ZoneOffset.UTC).toLocalDate() : null;
        LocalDate effTo = payload.effectiveTo() != null ? Instant.ofEpochMilli(payload.effectiveTo()).atZone(ZoneOffset.UTC).toLocalDate() : null;

        List<BomLine> lines = buildBomLines(payload.lines());
        BomHeader bom = new BomHeader(payload.bomCode(), payload.finishedItemId(), payload.finishedGoodName(),
                payload.yieldQuantity(), payload.revision(), effFrom, effTo, payload.notes(), payload.active(), lines);
        BomHeader saved = bomHeaderRepository.save(bom);
        auditService.record("CREATE", "BOM", saved.getId(), getCurrentUser(), "{\"bomCode\":\"" + saved.getBomCode() + "\"}", null);
        return toBomResponse(saved);
    }

    @Transactional
    public ManufacturingApi.BomResponse updateBom(String id, ManufacturingApi.BomPayload payload) {
        BomHeader bom = bomHeaderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("BOM not found", "MFG_BOM_NOT_FOUND"));
        LocalDate effFrom = payload.effectiveFrom() != null ? Instant.ofEpochMilli(payload.effectiveFrom()).atZone(ZoneOffset.UTC).toLocalDate() : null;
        LocalDate effTo = payload.effectiveTo() != null ? Instant.ofEpochMilli(payload.effectiveTo()).atZone(ZoneOffset.UTC).toLocalDate() : null;

        List<BomLine> lines = buildBomLines(payload.lines());
        bom.update(payload.bomCode(), payload.finishedItemId(), payload.finishedGoodName(),
                payload.yieldQuantity(), payload.revision(), effFrom, effTo, payload.notes(), payload.active(), lines);
        BomHeader saved = bomHeaderRepository.save(bom);
        auditService.record("UPDATE", "BOM", saved.getId(), getCurrentUser(), "{\"bomCode\":\"" + saved.getBomCode() + "\"}", null);
        return toBomResponse(saved);
    }

    // ─── Production Orders ──────────────────────────────────────────

    public List<ManufacturingApi.ProductionOrderResponse> listProductionOrders() {
        return productionOrderRepository.findAllByOrderByStartDateDescCreatedAtDesc().stream().map(this::toOrderResponse).toList();
    }

    @Transactional
    public ManufacturingApi.ProductionOrderResponse createProductionOrder(ManufacturingApi.ProductionOrderPayload payload) {
        if (productionOrderRepository.existsByOrderNumberIgnoreCase(payload.orderNumber().strip())) {
            throw new BusinessRuleException("رقم أمر الإنتاج مستخدم بالفعل.", "MFG_ORDER_NUMBER_EXISTS", HttpStatus.CONFLICT);
        }
        BomHeader bom = bomHeaderRepository.findById(payload.bomId())
                .orElseThrow(() -> new NotFoundException("قائمة المواد غير موجودة", "MFG_BOM_NOT_FOUND"));

        LocalDate startDate = Instant.ofEpochMilli(payload.startDate()).atZone(ZoneOffset.UTC).toLocalDate();
        ProductionOrder order = new ProductionOrder(payload.orderNumber(), bom.getId(), bom.getFinishedItemId(),
                bom.getRevision(), payload.targetQuantity(), startDate, payload.notes());
        ProductionOrder saved = productionOrderRepository.save(order);
        auditService.record("CREATE", "PRODUCTION_ORDER", saved.getId(), getCurrentUser(), "{\"orderNumber\":\"" + saved.getOrderNumber() + "\"}", null);
        return toOrderResponse(saved);
    }

    public ManufacturingApi.MaterialReadinessResponse checkMaterialReadiness(String id) {
        ProductionOrder order = requireOrder(id);
        BomHeader bom = bomHeaderRepository.findById(order.getBomId())
                .orElseThrow(() -> new NotFoundException("BOM not found", "MFG_BOM_NOT_FOUND"));

        BigDecimal yield = bom.getYieldQuantity() == null || bom.getYieldQuantity().signum() <= 0 ? BigDecimal.ONE : bom.getYieldQuantity();
        BigDecimal scale = order.getTargetQuantity().divide(yield, 6, RoundingMode.HALF_UP);

        boolean allAvailable = true;
        List<ManufacturingApi.MaterialRequirementView> views = new ArrayList<>();
        for (BomLine line : bom.getLines()) {
            BigDecimal wasteFactor = BigDecimal.ONE.add(line.getWastePercent() == null ? BigDecimal.ZERO : line.getWastePercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            BigDecimal required = line.getQuantityPer().multiply(scale).multiply(wasteFactor).setScale(2, RoundingMode.CEILING);
            BigDecimal stock = operationsService.stockBalance(line.getComponentItemId());
            BigDecimal shortage = required.subtract(stock).max(BigDecimal.ZERO);
            boolean ready = stock.compareTo(required) >= 0;
            if (!ready) allAvailable = false;
            views.add(new ManufacturingApi.MaterialRequirementView(line.getComponentItemId(), line.getComponentItemName(),
                    required, stock, shortage, ready));
        }
        return new ManufacturingApi.MaterialReadinessResponse(order.getId(), order.getOrderNumber(), allAvailable, views);
    }

    @Transactional
    public ManufacturingApi.ProductionOrderResponse startProductionOrder(String id) {
        ProductionOrder order = requireOrder(id);
        if (order.getStatus() != ProductionOrder.Status.PLANNED) {
            throw new BusinessRuleException("يمكن بدء أمر الإنتاج فقط من حالة مخطط.", "MFG_ORDER_START_FROM_PLANNED_ONLY", HttpStatus.CONFLICT);
        }
        BomHeader bom = bomHeaderRepository.findById(order.getBomId())
                .orElseThrow(() -> new NotFoundException("BOM not found", "MFG_BOM_NOT_FOUND"));

        if (bom.getLines().isEmpty()) {
            throw new BusinessRuleException("لا يمكن بدء أمر إنتاج بقائمة مواد خالية من المكونات.", "MFG_BOM_NO_LINES", HttpStatus.CONFLICT);
        }

        ManufacturingApi.MaterialReadinessResponse readiness = checkMaterialReadiness(id);
        if (!readiness.allMaterialsAvailable()) {
            throw new BusinessRuleException("نقص في الرصيد المتاح للمواد الخام المطلوبة لتنفيذ أمر الإنتاج.", "MFG_MATERIAL_SHORTAGE", HttpStatus.CONFLICT);
        }

        String actor = getCurrentUser();
        Instant occurredAt = Instant.now();

        // Post raw material issue movements & freeze BOM snapshot
        for (ManufacturingApi.MaterialRequirementView req : readiness.requirements()) {
            operationsService.recordProductionIssue(req.componentItemId(), req.requiredQuantity(),
                    order.getOrderNumber(), "Material issue for work order " + order.getOrderNumber(), occurredAt, actor);
            int version = 1;
            try { version = Integer.parseInt(bom.getRevision().replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            bomSnapshotService.captureBomSnapshot(order.getId(), bom.getId(), version, req.componentItemId(), req.requiredQuantity());
        }

        order.start();
        ProductionOrder saved = productionOrderRepository.save(order);
        auditService.record("START", "PRODUCTION_ORDER", saved.getId(), actor, "{\"orderNumber\":\"" + saved.getOrderNumber() + "\"}", null);
        return toOrderResponse(saved);
    }

    @Transactional
    public ManufacturingApi.ProductionOrderResponse completeProductionOrder(String id, ManufacturingApi.CompleteProductionOrderPayload payload) {
        ProductionOrder order = requireOrder(id);
        if (order.getStatus() != ProductionOrder.Status.IN_PROGRESS) {
            throw new BusinessRuleException("يمكن إكمال أمر الإنتاج فقط إذا كان قيد التنفيذ.", "MFG_ORDER_COMPLETE_FROM_IN_PROGRESS_ONLY", HttpStatus.CONFLICT);
        }

        if (payload.actualOutputQuantity() == null || payload.actualOutputQuantity().signum() <= 0) {
            throw new BusinessRuleException("كمية الإنتاج الفعلية يجب أن تكون أكبر من صفر.", "MFG_ACTUAL_OUTPUT_POSITIVE", HttpStatus.CONFLICT);
        }

        BomHeader bom = bomHeaderRepository.findById(order.getBomId()).orElse(null);
        String finishedItemId = order.getFinishedItemId() != null ? order.getFinishedItemId() : (bom != null ? bom.getFinishedItemId() : null);
        if (finishedItemId == null || finishedItemId.isBlank()) {
            throw new BusinessRuleException("يجب ربط الصنف التام بأمر الإنتاج أو قائمة المواد لاستلام المنتجات.", "MFG_FINISHED_ITEM_REQUIRED", HttpStatus.CONFLICT);
        }

        LocalDate completionDate = Instant.ofEpochMilli(payload.completionDate()).atZone(ZoneOffset.UTC).toLocalDate();
        String actor = getCurrentUser();
        Instant occurredAt = completionDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        // Derive actual material cost from issued components
        ManufacturingApi.MaterialReadinessResponse readiness = checkMaterialReadiness(id);
        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        for (ManufacturingApi.MaterialRequirementView req : readiness.requirements()) {
            BigDecimal unitCost = resolveLatestUnitCost(req.componentItemId());
            BigDecimal itemCost = req.requiredQuantity().multiply(unitCost == null ? BigDecimal.ZERO : unitCost);
            totalMaterialCost = totalMaterialCost.add(itemCost);
        }

        BigDecimal unitCost = totalMaterialCost.divide(payload.actualOutputQuantity(), 2, RoundingMode.HALF_UP);

        // Record finished goods receipt in inventory
        operationsService.recordProductionReceipt(finishedItemId, payload.actualOutputQuantity(), unitCost,
                order.getOrderNumber(), "Finished goods receipt from WO " + order.getOrderNumber(), occurredAt, actor);

        order.complete(payload.actualOutputQuantity(), payload.scrapQuantity(), totalMaterialCost, unitCost, completionDate, payload.notes());
        ProductionOrder saved = productionOrderRepository.save(order);
        auditService.record("COMPLETE", "PRODUCTION_ORDER", saved.getId(), actor, "{\"orderNumber\":\"" + saved.getOrderNumber() + "\",\"output\":" + payload.actualOutputQuantity() + "}", null);
        return toOrderResponse(saved);
    }

    @Transactional
    public ManufacturingApi.ProductionOrderResponse cancelProductionOrder(String id) {
        ProductionOrder order = requireOrder(id);
        if (order.getStatus() == ProductionOrder.Status.COMPLETED) {
            throw new BusinessRuleException("لا يمكن إلغاء أمر إنتاج م مكتمل.", "MFG_ORDER_CANNOT_CANCEL_COMPLETED", HttpStatus.CONFLICT);
        }
        if (order.getStatus() == ProductionOrder.Status.CANCELLED) {
            throw new BusinessRuleException("أمر الإنتاج ملغي بالفعل.", "MFG_ORDER_ALREADY_CANCELLED", HttpStatus.CONFLICT);
        }

        String actor = getCurrentUser();
        if (order.getStatus() == ProductionOrder.Status.IN_PROGRESS) {
            // Reverse raw material issues
            ManufacturingApi.MaterialReadinessResponse readiness = checkMaterialReadiness(id);
            for (ManufacturingApi.MaterialRequirementView req : readiness.requirements()) {
                BigDecimal unitCost = resolveLatestUnitCost(req.componentItemId());
                operationsService.recordProductionReceipt(req.componentItemId(), req.requiredQuantity(), unitCost,
                        order.getOrderNumber(), "Reversal of raw material issue for cancelled WO " + order.getOrderNumber(), Instant.now(), actor);
            }
        }

        order.cancel();
        ProductionOrder saved = productionOrderRepository.save(order);
        auditService.record("CANCEL", "PRODUCTION_ORDER", saved.getId(), actor, "{\"orderNumber\":\"" + saved.getOrderNumber() + "\"}", null);
        return toOrderResponse(saved);
    }

    // ─── Quality Control Inspections ────────────────────────────────

    public List<ManufacturingApi.QualityInspectionResponse> listInspections() {
        return qualityInspectionRepository.findAllByOrderByInspectionDateDescCreatedAtDesc().stream().map(this::toQualityResponse).toList();
    }

    @Transactional
    public ManufacturingApi.QualityInspectionResponse createInspection(ManufacturingApi.QualityInspectionPayload payload) {
        LocalDate date = Instant.ofEpochMilli(payload.inspectionDate()).atZone(ZoneOffset.UTC).toLocalDate();
        QualityInspection.Status status = QualityInspection.Status.valueOf(payload.status());
        QualityInspection qi = new QualityInspection(payload.inspectionNumber(), date, payload.sourceType(), payload.passedQuantity(), payload.failedQuantity(), status, payload.inspectorName(), payload.notes());
        QualityInspection saved = qualityInspectionRepository.save(qi);
        auditService.record("CREATE", "QUALITY_INSPECTION", saved.getId(), getCurrentUser(), "{\"inspectionNumber\":\"" + saved.getInspectionNumber() + "\"}", null);
        return toQualityResponse(saved);
    }

    // ─── Helpers & Mappers ──────────────────────────────────────────

    private BigDecimal resolveLatestUnitCost(String itemId) {
        return operationsService.latestUnitCost(itemId);
    }

    private ProductionOrder requireOrder(String id) {
        return productionOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("أمر الإنتاج غير موجود", "MFG_PRODUCTION_ORDER_NOT_FOUND"));
    }

    private List<BomLine> buildBomLines(List<ManufacturingApi.BomLinePayload> payloads) {
        if (payloads == null) return List.of();
        List<BomLine> lines = new ArrayList<>();
        int lineNo = 1;
        for (ManufacturingApi.BomLinePayload p : payloads) {
            lines.add(new BomLine(p.componentItemId(), p.componentItemName(), p.quantityPer(),
                    p.unitOfMeasure(), p.wastePercent(), p.lineNumber() > 0 ? p.lineNumber() : lineNo++));
        }
        return lines;
    }

    private ManufacturingApi.BomResponse toBomResponse(BomHeader bom) {
        Long effFromMs = bom.getEffectiveFrom() != null ? bom.getEffectiveFrom().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() : null;
        Long effToMs = bom.getEffectiveTo() != null ? bom.getEffectiveTo().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() : null;

        List<ManufacturingApi.BomLineResponse> lineResponses = bom.getLines().stream().map(l ->
                new ManufacturingApi.BomLineResponse(l.getId(), l.getComponentItemId(), l.getComponentItemName(),
                        l.getQuantityPer(), l.getUnitOfMeasure(), l.getWastePercent(), l.getLineNumber())).toList();

        return new ManufacturingApi.BomResponse(
                bom.getId(), bom.getBomCode(), bom.getFinishedItemId(), bom.getFinishedGoodName(),
                bom.getYieldQuantity(), bom.getRevision(), effFromMs, effToMs, bom.getNotes(), bom.isActive(),
                lineResponses, bom.getCreatedAt(), bom.getUpdatedAt()
        );
    }

    private ManufacturingApi.ProductionOrderResponse toOrderResponse(ProductionOrder order) {
        long startMs = order.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Long compMs = order.getCompletionDate() != null ? order.getCompletionDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() : null;
        return new ManufacturingApi.ProductionOrderResponse(
                order.getId(), order.getOrderNumber(), order.getBomId(), order.getFinishedItemId(),
                order.getBomRevision(), order.getTargetQuantity(), order.getActualOutputQuantity(),
                order.getScrapQuantity(), order.getActualMaterialCost(), order.getActualUnitCost(),
                startMs, compMs, order.getStatus().name(), order.getNotes(), order.getCreatedAt(),
                order.getUpdatedAt(), order.getVersion()
        );
    }

    private ManufacturingApi.QualityInspectionResponse toQualityResponse(QualityInspection qi) {
        long dateMs = qi.getInspectionDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ManufacturingApi.QualityInspectionResponse(
                qi.getId(), qi.getInspectionNumber(), dateMs, qi.getSourceType(),
                qi.getPassedQuantity(), qi.getFailedQuantity(), qi.getStatus().name(),
                qi.getInspectorName(), qi.getNotes(), qi.getCreatedAt()
        );
    }

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName() : "system";
    }
}
