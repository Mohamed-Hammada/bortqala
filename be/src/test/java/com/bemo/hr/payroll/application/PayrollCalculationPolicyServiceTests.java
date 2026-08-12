package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollCalculationPolicy;
import com.bemo.hr.payroll.infrastructure.PayrollCalculationPolicyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayrollCalculationPolicyServiceTests {
    private final PayrollCalculationPolicyRepository repository = mock(PayrollCalculationPolicyRepository.class);
    private final PayrollCalculationPolicyService service = new PayrollCalculationPolicyService(repository);

    @Test
    void selectsTheEffectiveTenantScopedPolicyDeterministically() {
        PayrollCalculationPolicy oldPolicy = new PayrollCalculationPolicy("Old", LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31), new BigDecimal("240"), new BigDecimal("1.5"));
        PayrollCalculationPolicy current = new PayrollCalculationPolicy("Current", LocalDate.of(2026, 1, 1),
                null, new BigDecimal("208"), new BigDecimal("2"));
        when(repository.findByActiveTrueOrderByEffectiveFromDesc()).thenReturn(List.of(current, oldPolicy));

        assertThat(service.effectivePolicy(LocalDate.of(2026, 8, 31))).isSameAs(current);
    }

    @Test
    void rejectsInvalidDivisorAndEffectiveDates() {
        assertThatThrownBy(() -> service.create("Bad", LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1), BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsOverlappingEffectivePolicies() {
        when(repository.findByActiveTrueOrderByEffectiveFromDesc()).thenReturn(List.of(
                new PayrollCalculationPolicy("Existing", LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 9, 30), new BigDecimal("240"), new BigDecimal("1.5"))));

        assertThatThrownBy(() -> service.create("Overlap", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31),
                new BigDecimal("208"), new BigDecimal("2")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(error -> ((BusinessRuleException) error).getCode())
                .isEqualTo("PAYROLL_POLICY_DATES_OVERLAP");
    }

    @Test
    void futurePolicyClosesTheCurrentOpenEndedPolicy() {
        PayrollCalculationPolicy current = new PayrollCalculationPolicy("Current", LocalDate.of(2026, 1, 1),
                null, new BigDecimal("240"), new BigDecimal("1.5"));
        when(repository.findByActiveTrueOrderByEffectiveFromDesc()).thenReturn(List.of(current));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollCalculationPolicy next = service.create("Next", LocalDate.of(2027, 1, 1), null,
                new BigDecimal("208"), new BigDecimal("2"));

        assertThat(current.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(next.getEffectiveFrom()).isEqualTo(LocalDate.of(2027, 1, 1));
        verify(repository, times(2)).save(any());
    }
}
