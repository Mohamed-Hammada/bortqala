package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.JournalApprovalRule;
import com.bemo.hr.finance.infrastructure.JournalApprovalRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JournalApprovalServiceTests {

    private JournalApprovalRuleRepository repository;
    private JournalApprovalService service;

    @BeforeEach
    void setUp() {
        repository = mock(JournalApprovalRuleRepository.class);
        service = new JournalApprovalService(repository);
    }

    @Test
    void setsRuleAndEvaluatesApprovalThresholdsSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JournalApprovalRule rule = service.setApprovalRule("acc-1", new BigDecimal("5000.00"), true);
        assertThat(rule).isNotNull();
        assertThat(rule.getMaxAmountWithoutApproval()).isEqualByComparingTo(new BigDecimal("5000.00"));

        when(repository.findByAccountId("acc-1")).thenReturn(Optional.of(rule));

        boolean requiresBelow = service.isApprovalRequired("acc-1", new BigDecimal("3000.00"));
        assertThat(requiresBelow).isFalse();

        boolean requiresAbove = service.isApprovalRequired("acc-1", new BigDecimal("10000.00"));
        assertThat(requiresAbove).isTrue();
        assertThat(service.isApprovalRequired("unconfigured-account", new BigDecimal("1.00"))).isTrue();
    }

    @Test
    void journalRequiresApprovalWhenAnyAccountRuleRequiresIt() {
        JournalApprovalRule exempt = new JournalApprovalRule("acc-1", new BigDecimal("5000.00"), true);
        JournalApprovalRule controlled = new JournalApprovalRule("acc-2", new BigDecimal("100.00"), true);
        when(repository.findByAccountId("acc-1")).thenReturn(Optional.of(exempt));
        when(repository.findByAccountId("acc-2")).thenReturn(Optional.of(controlled));

        assertThat(service.isApprovalRequired(java.util.Map.of(
                "acc-1", new BigDecimal("50.00"), "acc-2", new BigDecimal("150.00")))).isTrue();
    }
}
