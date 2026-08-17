package com.bemo.hr.trade.sales.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.trade.sales.domain.CustomerCreditProfile;
import com.bemo.hr.trade.sales.infrastructure.CustomerCreditProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerCreditServiceTests {

    private CustomerCreditProfileRepository repository;
    private CustomerCreditService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerCreditProfileRepository.class);
        service = new CustomerCreditService(repository);
    }

    @Test
    void setsLimitIncreasesExposureAndRejectsExcessExposure() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerCreditProfile profile = service.setCreditLimit("cust-1", new BigDecimal("1000.00"));
        assertThat(profile).isNotNull();
        assertThat(profile.getCreditLimit()).isEqualByComparingTo(new BigDecimal("1000.00"));

        when(repository.findByCustomerId("cust-1")).thenReturn(Optional.of(profile));

        service.checkAndIncreaseExposure("cust-1", new BigDecimal("600.00"));
        assertThat(profile.getCurrentExposure()).isEqualByComparingTo(new BigDecimal("600.00"));

        assertThatThrownBy(() -> service.checkAndIncreaseExposure("cust-1", new BigDecimal("500.00")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
