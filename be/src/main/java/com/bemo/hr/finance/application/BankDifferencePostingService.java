package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.treasury.BankDifferencePosting;
import com.bemo.hr.finance.infrastructure.BankDifferencePostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BankDifferencePostingService {

    private final BankDifferencePostingRepository repository;

    public BankDifferencePostingService(BankDifferencePostingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BankDifferencePosting postDifference(String statementLineId, BankDifferencePosting.DifferenceType differenceType, BigDecimal amount) {
        BankDifferencePosting posting = new BankDifferencePosting(statementLineId, differenceType, amount);
        return repository.save(posting);
    }
}
