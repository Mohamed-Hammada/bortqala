package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.domain.BomHeader;
import com.bemo.hr.manufacturing.production.domain.ProductionOrder;
import com.bemo.hr.manufacturing.production.domain.QualityInspection;
import com.bemo.hr.manufacturing.production.infrastructure.BomHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.QualityInspectionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/manufacturing")
public class ManufacturingController {

    private final BomHeaderRepository bomHeaderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final QualityInspectionRepository qualityInspectionRepository;

    public ManufacturingController(BomHeaderRepository bomHeaderRepository,
                                   ProductionOrderRepository productionOrderRepository,
                                   QualityInspectionRepository qualityInspectionRepository) {
        this.bomHeaderRepository = bomHeaderRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.qualityInspectionRepository = qualityInspectionRepository;
    }

    // --- BOMs ---
    @GetMapping("/boms")
    public List<ManufacturingApi.BomResponse> listBoms() {
        return bomHeaderRepository.findAllByOrderByBomCodeAsc().stream().map(this::toBomResponse).toList();
    }

    @PostMapping("/boms")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ManufacturingApi.BomResponse createBom(@Valid @RequestBody ManufacturingApi.BomPayload payload) {
        BomHeader bom = new BomHeader(payload.bomCode(), payload.finishedGoodName(), payload.yieldQuantity(), payload.notes(), payload.active());
        return toBomResponse(bomHeaderRepository.save(bom));
    }

    // --- Production Work Orders ---
    @GetMapping("/orders")
    public List<ManufacturingApi.ProductionOrderResponse> listProductionOrders() {
        return productionOrderRepository.findAllByOrderByStartDateDescCreatedAtDesc().stream().map(this::toOrderResponse).toList();
    }

    @PostMapping("/orders")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ManufacturingApi.ProductionOrderResponse createProductionOrder(@Valid @RequestBody ManufacturingApi.ProductionOrderPayload payload) {
        LocalDate startDate = Instant.ofEpochMilli(payload.startDate()).atZone(ZoneOffset.UTC).toLocalDate();
        ProductionOrder order = new ProductionOrder(payload.orderNumber(), payload.bomId(), payload.targetQuantity(), startDate);
        return toOrderResponse(productionOrderRepository.save(order));
    }

    @PostMapping("/orders/{id}/start")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ManufacturingApi.ProductionOrderResponse startProductionOrder(@PathVariable String id) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر الإنتاج غير موجود"));
        order.updateStatus(ProductionOrder.Status.IN_PROGRESS);
        return toOrderResponse(productionOrderRepository.save(order));
    }

    @PostMapping("/orders/{id}/complete")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ManufacturingApi.ProductionOrderResponse completeProductionOrder(@PathVariable String id) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("أمر الإنتاج غير موجود"));
        order.updateStatus(ProductionOrder.Status.COMPLETED);
        return toOrderResponse(productionOrderRepository.save(order));
    }

    // --- Quality Control Inspections ---
    @GetMapping("/quality")
    public List<ManufacturingApi.QualityInspectionResponse> listInspections() {
        return qualityInspectionRepository.findAllByOrderByInspectionDateDescCreatedAtDesc().stream().map(this::toQualityResponse).toList();
    }

    @PostMapping("/quality")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER')")
    public ManufacturingApi.QualityInspectionResponse createInspection(@Valid @RequestBody ManufacturingApi.QualityInspectionPayload payload) {
        LocalDate date = Instant.ofEpochMilli(payload.inspectionDate()).atZone(ZoneOffset.UTC).toLocalDate();
        QualityInspection.Status status = QualityInspection.Status.valueOf(payload.status());
        QualityInspection qi = new QualityInspection(payload.inspectionNumber(), date, payload.sourceType(), payload.passedQuantity(), payload.failedQuantity(), status, payload.inspectorName(), payload.notes());
        return toQualityResponse(qualityInspectionRepository.save(qi));
    }

    // --- Converters ---
    private ManufacturingApi.BomResponse toBomResponse(BomHeader bom) {
        return new ManufacturingApi.BomResponse(
                bom.getId(), bom.getBomCode(), bom.getFinishedGoodName(),
                bom.getYieldQuantity(), bom.getNotes(), bom.isActive(),
                bom.getCreatedAt(), bom.getUpdatedAt()
        );
    }

    private ManufacturingApi.ProductionOrderResponse toOrderResponse(ProductionOrder order) {
        long startMs = order.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return new ManufacturingApi.ProductionOrderResponse(
                order.getId(), order.getOrderNumber(), order.getBomId(),
                order.getTargetQuantity(), startMs, order.getStatus().name(),
                order.getCreatedAt(), order.getUpdatedAt()
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
}
