package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.rules.AccountingRulePolicy;
import com.bemo.hr.finance.infrastructure.AccountingRulePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeterministicPolicyEngineServiceTests {

    private AccountingRulePolicyRepository repository;
    private DeterministicPolicyEngineService service;

    @BeforeEach
    void setUp() {
        repository = mock(AccountingRulePolicyRepository.class);
        service = new DeterministicPolicyEngineService(repository);
    }

    @Test
    void createsAndEvaluatesAccountingPolicySuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountingRulePolicy policy = service.createPolicy("AP_INVOICE_CLEAR", "Supplier Invoice AP Clear", "INVOICE_ISSUED", "51000", "21000");
        assertThat(policy).isNotNull();
        assertThat(policy.getPolicyCode()).isEqualTo("AP_INVOICE_CLEAR");

        when(repository.findByPolicyCode("AP_INVOICE_CLEAR")).thenReturn(Optional.of(policy));

        DeterministicPolicyEngineService.PolicyEvaluationResult eval = service.evaluatePolicy("AP_INVOICE_CLEAR", new BigDecimal("5000.00"));
        assertThat(eval.debitAccount()).isEqualTo("51000");
        assertThat(eval.creditAccount()).isEqualTo("21000");
        assertThat(eval.amount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }
}
