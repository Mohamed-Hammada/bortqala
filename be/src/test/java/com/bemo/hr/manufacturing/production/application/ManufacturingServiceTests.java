package com.bemo.hr.manufacturing.production.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.manufacturing.production.api.ManufacturingApi;
import com.bemo.hr.manufacturing.production.domain.BomHeader;
import com.bemo.hr.manufacturing.production.domain.BomLine;
import com.bemo.hr.manufacturing.production.domain.ProductionOrder;
import com.bemo.hr.manufacturing.production.infrastructure.BomHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.BomLineRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.QualityInspectionRepository;
import com.bemo.hr.operations.OperationsService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturingServiceTests {

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Mock private BomHeaderRepository bomHeaderRepository;
    @Mock private BomLineRepository bomLineRepository;
    @Mock private ProductionOrderRepository productionOrderRepository;
    @Mock private QualityInspectionRepository qualityInspectionRepository;
    @Mock private OperationsService operationsService;
    @Mock private AuditService auditService;

    @InjectMocks
    private ManufacturingService service;

    @Test
    void createBom_savesHeaderAndLines() {
        when(bomHeaderRepository.existsByBomCodeIgnoreCase("BOM-001")).thenReturn(false);
        when(bomHeaderRepository.save(any(BomHeader.class))).thenAnswer(i -> i.getArgument(0));

        ManufacturingApi.BomLinePayload line = new ManufacturingApi.BomLinePayload(
                "rm-1", "Raw Material 1", BigDecimal.valueOf(2), "KG", BigDecimal.ZERO, 1);
        ManufacturingApi.BomPayload payload = new ManufacturingApi.BomPayload(
                "BOM-001", "fg-1", "Finished Good 1", BigDecimal.ONE, "v1.0", null, null, "Notes", true, List.of(line));

        ManufacturingApi.BomResponse response = service.createBom(payload);

        assertThat(response).isNotNull();
        assertThat(response.bomCode()).isEqualTo("BOM-001");
        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).componentItemId()).isEqualTo("rm-1");
    }

    @Test
    void startProductionOrder_issuesRawMaterialsAndChangesStatus() {
        BomLine line = new BomLine("rm-1", "Raw Material 1", BigDecimal.valueOf(2), "KG", BigDecimal.ZERO, 1);
        BomHeader bom = new BomHeader("BOM-001", "fg-1", "Finished Good 1", BigDecimal.ONE, "v1.0", null, null, null, true, List.of(line));
        ProductionOrder order = new ProductionOrder("WO-100", bom.getId(), "fg-1", "v1.0", BigDecimal.valueOf(5), LocalDate.now(), null);

        when(productionOrderRepository.findById("wo-1")).thenReturn(Optional.of(order));
        when(bomHeaderRepository.findById(bom.getId())).thenReturn(Optional.of(bom));
        when(operationsService.stockBalance("rm-1")).thenReturn(BigDecimal.valueOf(100));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(i -> i.getArgument(0));

        ManufacturingApi.ProductionOrderResponse response = service.startProductionOrder("wo-1");

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        verify(operationsService).recordProductionIssue(eq("rm-1"), eq(BigDecimal.valueOf(10.00).setScale(2)), eq("WO-100"), anyString(), any(), any());
    }

    @Test
    void startProductionOrder_shortageInStock_throwsException() {
        BomLine line = new BomLine("rm-1", "Raw Material 1", BigDecimal.valueOf(2), "KG", BigDecimal.ZERO, 1);
        BomHeader bom = new BomHeader("BOM-001", "fg-1", "Finished Good 1", BigDecimal.ONE, "v1.0", null, null, null, true, List.of(line));
        ProductionOrder order = new ProductionOrder("WO-100", bom.getId(), "fg-1", "v1.0", BigDecimal.valueOf(5), LocalDate.now(), null);

        when(productionOrderRepository.findById("wo-1")).thenReturn(Optional.of(order));
        when(bomHeaderRepository.findById(bom.getId())).thenReturn(Optional.of(bom));
        when(operationsService.stockBalance("rm-1")).thenReturn(BigDecimal.valueOf(2));

        assertThatThrownBy(() -> service.startProductionOrder("wo-1"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("MFG_MATERIAL_SHORTAGE");
    }

    @Test
    void completeProductionOrder_receivesFinishedGoodsAndComputesCost() {
        BomLine line = new BomLine("rm-1", "Raw Material 1", BigDecimal.valueOf(2), "KG", BigDecimal.ZERO, 1);
        BomHeader bom = new BomHeader("BOM-001", "fg-1", "Finished Good 1", BigDecimal.ONE, "v1.0", null, null, null, true, List.of(line));
        ProductionOrder order = new ProductionOrder("WO-100", bom.getId(), "fg-1", "v1.0", BigDecimal.valueOf(5), LocalDate.now(), null);
        order.start();

        when(productionOrderRepository.findById("wo-1")).thenReturn(Optional.of(order));
        when(bomHeaderRepository.findById(bom.getId())).thenReturn(Optional.of(bom));
        when(operationsService.stockBalance("rm-1")).thenReturn(BigDecimal.valueOf(100));
        when(operationsService.latestUnitCost("rm-1")).thenReturn(BigDecimal.valueOf(15));
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(i -> i.getArgument(0));

        ManufacturingApi.CompleteProductionOrderPayload payload = new ManufacturingApi.CompleteProductionOrderPayload(
                BigDecimal.valueOf(5), BigDecimal.ZERO, System.currentTimeMillis(), "Order complete");

        ManufacturingApi.ProductionOrderResponse response = service.completeProductionOrder("wo-1", payload);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.actualOutputQuantity()).isEqualTo(BigDecimal.valueOf(5));
        verify(operationsService).recordProductionReceipt(eq("fg-1"), eq(BigDecimal.valueOf(5)), eq(BigDecimal.valueOf(30.00).setScale(2)), eq("WO-100"), anyString(), any(), any());
    }
}
