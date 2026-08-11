package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.treasury.BankDifferencePosting;
import com.bemo.hr.finance.infrastructure.BankDifferencePostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankDifferencePostingServiceTests {

    private BankDifferencePostingRepository repository;
    private BankDifferencePostingService service;

    @BeforeEach
    void setUp() {
        repository = mock(BankDifferencePostingRepository.class);
        service = new BankDifferencePostingService(repository);
    }

    @Test
    void postsBankFeeDifferenceSuccessfully() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankDifferencePosting posting = service.postDifference("stmt-line-10", BankDifferencePosting.DifferenceType.FEE, new BigDecimal("45.00"));
        assertThat(posting).isNotNull();
        assertThat(posting.getDifferenceType()).isEqualTo(BankDifferencePosting.DifferenceType.FEE);
        assertThat(posting.getAmount()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(posting.getStatus()).isEqualTo(BankDifferencePosting.Status.POSTED);
    }
}
