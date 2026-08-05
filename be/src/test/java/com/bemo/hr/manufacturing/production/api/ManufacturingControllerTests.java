package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.infrastructure.BomHeaderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.ProductionOrderRepository;
import com.bemo.hr.manufacturing.production.infrastructure.QualityInspectionRepository;
import com.bemo.hr.shared.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ManufacturingControllerTests {

    @Mock private BomHeaderRepository bomHeaderRepository;
    @Mock private ProductionOrderRepository productionOrderRepository;
    @Mock private QualityInspectionRepository qualityInspectionRepository;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }


    @InjectMocks
    private ManufacturingController controller;

    @Test
    void listBoms_returnsEmptyList_whenNoBomsExist() {
        org.mockito.Mockito.when(bomHeaderRepository.findAllByOrderByBomCodeAsc()).thenReturn(java.util.Collections.emptyList());
        assertThat(controller.listBoms()).isEmpty();
    }
    
    @Test
    void createBom_savesAndReturnsBom() {
        ManufacturingApi.BomPayload payload = new ManufacturingApi.BomPayload("BOM1", "FG", java.math.BigDecimal.ONE, "Notes", true);
        com.bemo.hr.manufacturing.production.domain.BomHeader saved = new com.bemo.hr.manufacturing.production.domain.BomHeader("BOM1", "FG", java.math.BigDecimal.ONE, "Notes", true);
        org.mockito.Mockito.when(bomHeaderRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(saved);
        
        ManufacturingApi.BomResponse response = controller.createBom(payload);
        assertThat(response.bomCode()).isEqualTo("BOM1");
    }

}
