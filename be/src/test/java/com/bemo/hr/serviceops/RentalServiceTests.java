package com.bemo.hr.serviceops;

import com.bemo.hr.serviceops.api.ServiceOpsApi;
import com.bemo.hr.serviceops.application.RentalService;
import com.bemo.hr.serviceops.domain.RentalContract;
import com.bemo.hr.serviceops.domain.RentalItem;
import com.bemo.hr.serviceops.infrastructure.RentalContractLineRepository;
import com.bemo.hr.serviceops.infrastructure.RentalContractRepository;
import com.bemo.hr.serviceops.infrastructure.RentalItemRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTests {

    @Mock
    private RentalItemRepository itemRepository;

    @Mock
    private RentalContractRepository contractRepository;

    @Mock
    private RentalContractLineRepository lineRepository;

    private RentalService rentalService;

    private static final String APP_ID = "test-app";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        rentalService = new RentalService(itemRepository, contractRepository, lineRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createItem_persistsAndReturnsResponse() {
        ServiceOpsApi.RentalItemCreateRequest request = new ServiceOpsApi.RentalItemCreateRequest(
                "GEN-001", "Generator 50kVA", "Generator 50kVA", "Power",
                new BigDecimal("500.00"), new BigDecimal("2800.00"), new BigDecimal("10000.00"), new BigDecimal("2000.00")
        );

        when(itemRepository.save(any(RentalItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.RentalItemResponse response = rentalService.createItem(request);

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo("GEN-001");
        assertThat(response.status()).isEqualTo(RentalItem.Status.AVAILABLE);
        verify(itemRepository).save(any(RentalItem.class));
    }

    @Test
    void calculateContractCharges_weeklyBillingRule() {
        RentalContract contract = new RentalContract(
                APP_ID, "CNT-001", "party-1",
                "2026-08-01", "2026-08-13", RentalContract.RateUnit.WEEK,
                new BigDecimal("700.00"), BigDecimal.ZERO, "Notes"
        );

        // 12 days: 1 week (700) + 5 days (5 * 100 = 500) = 1200.00
        BigDecimal charges = rentalService.calculateContractCharges(contract, "2026-08-13");
        assertThat(charges).isEqualByComparingTo("1200.00");
    }

    @Test
    void calculateContractCharges_dailyBillingRule() {
        RentalContract contract = new RentalContract(
                APP_ID, "CNT-002", "party-1",
                "2026-08-01", "2026-08-05", RentalContract.RateUnit.DAY,
                new BigDecimal("150.00"), BigDecimal.ZERO, "Notes"
        );

        // 4 days: 4 * 150 = 600.00
        BigDecimal charges = rentalService.calculateContractCharges(contract, "2026-08-05");
        assertThat(charges).isEqualByComparingTo("600.00");
    }

    @Test
    void activateContract_updatesItemsToRented() {
        RentalItem item = new RentalItem(APP_ID, "GEN-01", "Gen", "Gen", "Power",
                new BigDecimal("100"), new BigDecimal("600"), new BigDecimal("2000"), BigDecimal.ZERO);
        RentalContract contract = new RentalContract(APP_ID, "CNT-01", "party-1",
                "2026-08-01", "2026-08-05", RentalContract.RateUnit.DAY, new BigDecimal("100"), BigDecimal.ZERO, "Notes");
        contract.addLine(new com.bemo.hr.serviceops.domain.RentalContractLine(APP_ID, item.getId(), BigDecimal.ONE, new BigDecimal("100")));

        when(contractRepository.findByAppIdAndId(APP_ID, contract.getId())).thenReturn(Optional.of(contract));
        when(itemRepository.findByAppIdAndId(APP_ID, item.getId())).thenReturn(Optional.of(item));
        when(contractRepository.save(any(RentalContract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.RentalContractResponse response = rentalService.activateContract(contract.getId());

        assertThat(response.status()).isEqualTo(RentalContract.Status.ACTIVE);
        assertThat(item.getStatus()).isEqualTo(RentalItem.Status.RENTED);
    }

    @Test
    void returnAndCloseContract_releasesItemsAndAppliesDamageFee() {
        RentalItem item = new RentalItem(APP_ID, "GEN-01", "Gen", "Gen", "Power",
                new BigDecimal("100"), new BigDecimal("600"), new BigDecimal("2000"), BigDecimal.ZERO);
        item.setStatus(RentalItem.Status.RENTED);

        RentalContract contract = new RentalContract(APP_ID, "CNT-01", "party-1",
                "2026-08-01", "2026-08-05", RentalContract.RateUnit.DAY, new BigDecimal("100"), BigDecimal.ZERO, "Notes");
        contract.setStatus(RentalContract.Status.ACTIVE);
        contract.addLine(new com.bemo.hr.serviceops.domain.RentalContractLine(APP_ID, item.getId(), BigDecimal.ONE, new BigDecimal("100")));

        when(contractRepository.findByAppIdAndId(APP_ID, contract.getId())).thenReturn(Optional.of(contract));
        when(itemRepository.findByAppIdAndId(APP_ID, item.getId())).thenReturn(Optional.of(item));
        when(contractRepository.save(any(RentalContract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOpsApi.ReturnRentalContractRequest returnReq = new ServiceOpsApi.ReturnRentalContractRequest(
                "2026-08-05", new BigDecimal("50.00"), "Minor scratch"
        );

        ServiceOpsApi.RentalContractResponse response = rentalService.returnAndCloseContract(contract.getId(), returnReq);

        assertThat(response.status()).isEqualTo(RentalContract.Status.CLOSED);
        assertThat(response.damageFee()).isEqualByComparingTo("50.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("450.00"); // 4 days @ 100 = 400 + 50 damage
        assertThat(item.getStatus()).isEqualTo(RentalItem.Status.AVAILABLE);
    }

    @Test
    void getUtilizationSummary_computesPercentage() {
        when(itemRepository.countByAppId(APP_ID)).thenReturn(10L);
        when(itemRepository.countByAppIdAndStatus(APP_ID, RentalItem.Status.RENTED)).thenReturn(6L);
        when(itemRepository.countByAppIdAndStatus(APP_ID, RentalItem.Status.AVAILABLE)).thenReturn(4L);

        ServiceOpsApi.RentalUtilizationSummary summary = rentalService.getUtilizationSummary();

        assertThat(summary.totalItems()).isEqualTo(10L);
        assertThat(summary.rentedItems()).isEqualTo(6L);
        assertThat(summary.availableItems()).isEqualTo(4L);
        assertThat(summary.utilizationPercentage()).isEqualTo(60.0);
    }
}
