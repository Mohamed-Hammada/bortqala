package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.treasury.BankDifferencePosting;
import com.bemo.hr.finance.infrastructure.BankDifferencePostingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class BankDifferencePostingService {

    private final BankDifferencePostingRepository repository;

    public BankDifferencePostingService(BankDifferencePostingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BankDifferencePosting postDifference(String statementLineId, BankDifferencePosting.DifferenceType differenceType, BigDecimal amount) {
        log.debug("postDifference called with statementLineId={}, differenceType={}, amount={}", statementLineId, differenceType, amount);
        BankDifferencePosting posting = new BankDifferencePosting(statementLineId, differenceType, amount);
        BankDifferencePosting saved = repository.save(posting);
        log.info("BankDifferencePosting {} posted successfully", saved.getId());
        return saved;
    }
}
