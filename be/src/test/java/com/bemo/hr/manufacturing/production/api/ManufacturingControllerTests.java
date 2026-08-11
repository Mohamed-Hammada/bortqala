package com.bemo.hr.manufacturing.production.api;

import com.bemo.hr.manufacturing.production.application.ManufacturingService;
import com.bemo.hr.shared.security.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturingControllerTests {

    @Mock private ManufacturingService manufacturingService;

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
        when(manufacturingService.listBoms()).thenReturn(Collections.emptyList());
        assertThat(controller.listBoms()).isEmpty();
    }

    @Test
    void createBom_savesAndReturnsBom() {
        ManufacturingApi.BomPayload payload = new ManufacturingApi.BomPayload(
                "BOM1", "item-1", "FG", BigDecimal.ONE, "v1.0", null, null, "Notes", true, List.of());
        ManufacturingApi.BomResponse saved = new ManufacturingApi.BomResponse(
                "bom-1", "BOM1", "item-1", "FG", BigDecimal.ONE, "v1.0", null, null, "Notes", true, List.of(), System.currentTimeMillis(), System.currentTimeMillis());

        when(manufacturingService.createBom(any())).thenReturn(saved);

        ManufacturingApi.BomResponse response = controller.createBom(payload);
        assertThat(response.bomCode()).isEqualTo("BOM1");
    }
}
