package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.domain.PayrollCalculationPolicy;
import com.bemo.hr.payroll.infrastructure.PayrollCalculationPolicyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PayrollCalculationPolicyService {
    private static final BigDecimal INITIAL_WORKING_HOUR_DIVISOR = new BigDecimal("240");
    private static final BigDecimal INITIAL_OVERTIME_MULTIPLIER = new BigDecimal("1.5");
    private final PayrollCalculationPolicyRepository payrollCalculationPolicyRepository;

    public PayrollCalculationPolicyService(PayrollCalculationPolicyRepository payrollCalculationPolicyRepository) {
        this.payrollCalculationPolicyRepository = payrollCalculationPolicyRepository;
    }

    private static boolean rangesOverlap(LocalDate leftStart, LocalDate leftEnd, LocalDate rightStart, LocalDate rightEnd) {
        if (leftStart == null) return false;
        LocalDate lEnd = leftEnd == null ? LocalDate.MAX : leftEnd;
        LocalDate rEnd = rightEnd == null ? LocalDate.MAX : rightEnd;
        return !lEnd.isBefore(rightStart) && !rEnd.isBefore(leftStart);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public PayrollCalculationPolicy effectivePolicy(LocalDate date) {
        List<PayrollCalculationPolicy> policies = payrollCalculationPolicyRepository.findByActiveTrueOrderByEffectiveFromDesc();
        return policies.stream().filter(policy -> policy.appliesOn(date)).findFirst()
                .orElseGet(() -> payrollCalculationPolicyRepository.save(new PayrollCalculationPolicy(
                        "Initial standard payroll policy", LocalDate.of(2000, 1, 1), null,
                        INITIAL_WORKING_HOUR_DIVISOR, INITIAL_OVERTIME_MULTIPLIER)));
    }

    public List<PayrollCalculationPolicy> list() {
        return payrollCalculationPolicyRepository.findByActiveTrueOrderByEffectiveFromDesc();
    }

    @Transactional
    public PayrollCalculationPolicy create(String name, LocalDate effectiveFrom, LocalDate effectiveTo,
                                           BigDecimal divisor, BigDecimal multiplier) {
        List<PayrollCalculationPolicy> policies = payrollCalculationPolicyRepository.findByActiveTrueOrderByEffectiveFromDesc();
        policies.stream()
                .filter(existing -> existing.getEffectiveTo() == null && effectiveFrom != null
                        && effectiveFrom.isAfter(existing.getEffectiveFrom()))
                .findFirst()
                .ifPresent(existing -> {
                    existing.closeBefore(effectiveFrom);
                    payrollCalculationPolicyRepository.save(existing);
                });
        boolean overlaps = policies.stream()
                .anyMatch(existing -> rangesOverlap(effectiveFrom, effectiveTo, existing.getEffectiveFrom(), existing.getEffectiveTo()));
        if (overlaps) {
            throw new BusinessRuleException("Payroll policy effective dates overlap an active policy.",
                    "PAYROLL_POLICY_DATES_OVERLAP", HttpStatus.CONFLICT);
        }
        return payrollCalculationPolicyRepository.save(
                new PayrollCalculationPolicy(name, effectiveFrom, effectiveTo, divisor, multiplier));
    }
}
