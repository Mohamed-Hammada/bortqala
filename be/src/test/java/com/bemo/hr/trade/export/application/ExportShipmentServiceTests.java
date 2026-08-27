package com.bemo.hr.trade.export.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.export.api.ExportShipmentApi;
import com.bemo.hr.trade.export.domain.*;
import com.bemo.hr.trade.export.infrastructure.ComplianceRegisterRepository;
import com.bemo.hr.trade.export.infrastructure.ExportShipmentRepository;
import com.bemo.hr.trade.export.infrastructure.PesticideRegisterRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportShipmentServiceTests {

    private static final String APP_ID = "app-1";

    @Mock
    private ExportShipmentRepository shipmentRepository;
    @Mock
    private ComplianceRegisterRepository complianceRegisterRepository;
    @Mock
    private PesticideRegisterRepository pesticideRegisterRepository;

    private ExportShipmentService service;

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        service = new ExportShipmentService(shipmentRepository, complianceRegisterRepository, pesticideRegisterRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ─── Status Transition Tests ─────────────────────────────────────

    @Test
    void transition_validSequence_preparingToBooked() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.PREPARING);
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.transitionShipment("sh-1", "BOOKED");
        assertThat(result.status()).isEqualTo("BOOKED");
    }

    @Test
    void transition_validSequence_bookedToShipped() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.BOOKED);
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.transitionShipment("sh-1", "SHIPPED");
        assertThat(result.status()).isEqualTo("SHIPPED");
    }

    @Test
    void transition_validSequence_shippedToSettled() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.SHIPPED);
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.transitionShipment("sh-1", "SETTLED");
        assertThat(result.status()).isEqualTo("SETTLED");
    }

    @Test
    void transition_invalidJump_rejected() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.PREPARING);
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        assertThatThrownBy(() -> service.transitionShipment("sh-1", "SHIPPED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void transition_fromSettled_rejected() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.SETTLED);
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        assertThatThrownBy(() -> service.transitionShipment("sh-1", "PREPARING"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SETTLED is terminal");
    }

    // ─── Compliance / PHI Violation Tests ────────────────────────────

    @Test
    void complianceCheck_phiViolation_detected() {
        LocalDate treatmentDate = LocalDate.of(2026, 8, 1);
        int phiDays = 14;
        LocalDate pickupDate = LocalDate.of(2026, 8, 11); // 10 days after — VIOLATION

        ComplianceRegister log = new ComplianceRegister("LOT-A", "Chlorpyrifos", treatmentDate, phiDays);
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-A"))
                .thenReturn(List.of(log));

        var result = service.checkCompliance(List.of("LOT-A"), pickupDate);
        assertThat(result.violations()).hasSize(1);
        assertThat(result.violations().get(0).lotReference()).isEqualTo("LOT-A");
        assertThat(result.violations().get(0).daysShort()).isEqualTo(4);
    }

    @Test
    void complianceCheck_compliantLot_noViolation() {
        LocalDate treatmentDate = LocalDate.of(2026, 8, 1);
        int phiDays = 14;
        LocalDate pickupDate = LocalDate.of(2026, 8, 16); // 15 days after — COMPLIANT

        ComplianceRegister log = new ComplianceRegister("LOT-B", "Mancozeb", treatmentDate, phiDays);
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-B"))
                .thenReturn(List.of(log));

        var result = service.checkCompliance(List.of("LOT-B"), pickupDate);
        assertThat(result.violations()).isEmpty();
        assertThat(result.totalLotsChecked()).isEqualTo(1);
    }

    @Test
    void complianceCheck_multipleLots_mixedResults() {
        LocalDate treatmentDate = LocalDate.of(2026, 8, 1);
        int phiDays = 14;

        ComplianceRegister violationLog = new ComplianceRegister("LOT-V", "Chlorpyrifos", treatmentDate, phiDays);
        ComplianceRegister compliantLog = new ComplianceRegister("LOT-C", "Mancozeb", LocalDate.of(2026, 7, 20), phiDays);

        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-V"))
                .thenReturn(List.of(violationLog));
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-C"))
                .thenReturn(List.of(compliantLog));

        LocalDate pickupDate = LocalDate.of(2026, 8, 11);
        var result = service.checkCompliance(List.of("LOT-V", "LOT-C"), pickupDate);
        assertThat(result.violations()).hasSize(1);
        assertThat(result.violations().get(0).lotReference()).isEqualTo("LOT-V");
        assertThat(result.totalLotsChecked()).isEqualTo(2);
    }

    @Test
    void complianceCheck_emptyLotList() {
        var result = service.checkCompliance(List.of(), LocalDate.now());
        assertThat(result.violations()).isEmpty();
        assertThat(result.totalLotsChecked()).isEqualTo(0);
    }

    @Test
    void complianceCheck_noTreatmentsForLot() {
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-EMPTY"))
                .thenReturn(List.of());

        var result = service.checkCompliance(List.of("LOT-EMPTY"), LocalDate.now());
        assertThat(result.violations()).isEmpty();
        assertThat(result.totalLotsChecked()).isEqualTo(0);
    }

    // ─── ComplianceRegister Domain Logic ─────────────────────────────

    @Test
    void complianceRegister_earliestSafePickup() {
        ComplianceRegister reg = new ComplianceRegister("LOT-X", "Chem", LocalDate.of(2026, 8, 1), 14);
        assertThat(reg.earliestSafePickup()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void complianceRegister_violationBeforeSafeDate() {
        ComplianceRegister reg = new ComplianceRegister("LOT-X", "Chem", LocalDate.of(2026, 8, 1), 14);
        assertThat(reg.isViolation(LocalDate.of(2026, 8, 10))).isTrue();
        assertThat(reg.isViolation(LocalDate.of(2026, 8, 15))).isFalse();
        assertThat(reg.isViolation(LocalDate.of(2026, 8, 16))).isFalse();
    }

    // ─── Shipment CRUD Tests ────────────────────────────────────────

    @Test
    void createShipment_generatesNumberAndLines() {
        when(shipmentRepository.save(any())).thenAnswer(inv -> {
            ExportShipment s = inv.getArgument(0);
            return s;
        });

        ExportShipmentApi.ShipmentPayload payload = new ExportShipmentApi.ShipmentPayload(
                "party-1", "Acme Corp", "CTR-001", "CONT-123", "BK-456", "ACID-789",
                "Alexandria", "Rotterdam", 1000L, 2000L, "notes",
                BigDecimal.valueOf(50000), "USD",
                List.of(
                        new ExportShipmentApi.ShipmentLinePayload(1, "Tomatoes", null, "LOT-T1", BigDecimal.valueOf(1000), "KG", null, null, 10),
                        new ExportShipmentApi.ShipmentLinePayload(2, "Cucumbers", null, "LOT-C1", BigDecimal.valueOf(500), "KG", null, null, 5)
                ));

        var result = service.createShipment(payload);
        assertThat(result.shipmentNumber()).startsWith("EXP-");
        assertThat(result.status()).isEqualTo("PREPARING");
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines().get(0).lotReference()).isEqualTo("LOT-T1");
    }

    @Test
    void updateShipment_onlyAllowedInPreparing() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.BOOKED);
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        ExportShipmentApi.ShipmentPayload payload = new ExportShipmentApi.ShipmentPayload(
                "party-1", null, null, null, null, null, null, null, null, null, null,
                null, null, List.of());

        assertThatThrownBy(() -> service.updateShipment("sh-1", payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Can only update");
    }

    // ─── Pesticide Register Tests ───────────────────────────────────

    @Test
    void createPesticide_duplicateRejected() {
        when(pesticideRegisterRepository.existsByChemicalNameIgnoreCase("Chlorpyrifos")).thenReturn(true);

        ExportShipmentApi.PesticidePayload payload = new ExportShipmentApi.PesticidePayload(
                "Chlorpyrifos", "Chlorpyrifos-methyl", "REG-001",
                BigDecimal.valueOf(0.5), "2 L/ha", 14, "Tomatoes", null);

        assertThatThrownBy(() -> service.createPesticide(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void createPesticide_success() {
        when(pesticideRegisterRepository.existsByChemicalNameIgnoreCase("Mancozeb")).thenReturn(false);
        when(pesticideRegisterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExportShipmentApi.PesticidePayload payload = new ExportShipmentApi.PesticidePayload(
                "Mancozeb", "Mancozeb 80%", "REG-002",
                BigDecimal.valueOf(1.0), "2.5 kg/ha", 7, "Cucumbers", null);

        var result = service.createPesticide(payload);
        assertThat(result.chemicalName()).isEqualTo("Mancozeb");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    // ─── Aging Tests ─────────────────────────────────────────────────

    @Test
    void aging_openShipmentsListed() {
        ExportShipment sh1 = createShipment(ExportShipmentStatus.BOOKED);
        sh1.setCustomerPartyId("p1");
        sh1.setCustomerPartyName("Acme");
        sh1.setExpectedFxAmount(BigDecimal.valueOf(10000));
        sh1.setExpectedFxCurrency("USD");
        sh1.setCreatedAt(System.currentTimeMillis() - 86400000L * 10); // 10 days ago

        ExportShipment sh2 = createShipment(ExportShipmentStatus.SHIPPED);
        sh2.setCustomerPartyId("p2");
        sh2.setCustomerPartyName("Beta");
        sh2.setExpectedFxAmount(BigDecimal.valueOf(20000));
        sh2.setExpectedFxCurrency("USD");
        sh2.setCreatedAt(System.currentTimeMillis() - 86400000L * 5); // 5 days ago

        when(shipmentRepository.findByStatusIn(List.of(ExportShipmentStatus.PREPARING, ExportShipmentStatus.BOOKED, ExportShipmentStatus.SHIPPED)))
                .thenReturn(List.of(sh1, sh2));

        var result = service.getAging();
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().get(0).daysOutstanding()).isGreaterThanOrEqualTo(10);
        assertThat(result.totalExpectedFx()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }

    @Test
    void aging_settledShipmentsExcluded() {
        ExportShipment sh = createShipment(ExportShipmentStatus.SETTLED);
        when(shipmentRepository.findByStatusIn(List.of(ExportShipmentStatus.PREPARING, ExportShipmentStatus.BOOKED, ExportShipmentStatus.SHIPPED)))
                .thenReturn(List.of());

        var result = service.getAging();
        assertThat(result.entries()).isEmpty();
    }

    // ─── Proceeds Test ───────────────────────────────────────────────

    @Test
    void recordProceeds_persistsRealizedAmount() {
        ExportShipment shipment = createShipment(ExportShipmentStatus.SHIPPED);
        shipment.setExpectedFxAmount(BigDecimal.valueOf(50000));
        shipment.setExpectedFxCurrency("USD");
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.recordProceeds("sh-1",
                new ExportShipmentApi.ProceedsPayload(BigDecimal.valueOf(52000)));
        assertThat(result.realizedFxAmount()).isEqualByComparingTo(BigDecimal.valueOf(52000));
        assertThat(result.expectedFxAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private ExportShipment createShipment(ExportShipmentStatus status) {
        ExportShipment s = new ExportShipment("EXP-20260826-TEST", "party-1", "Test Customer");
        try {
            var field = ExportShipment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(s, "sh-1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        s.setStatus(status);
        return s;
    }
}
