package com.bemo.hr.workforce.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.workforce.domain.WorkforceSettlementSnapshot;
import com.bemo.hr.workforce.infrastructure.WorkforceSettlementSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WorkforceSettlementSnapshotService {

    private final WorkforceSettlementSnapshotRepository repository;

    public WorkforceSettlementSnapshotService(WorkforceSettlementSnapshotRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkforceSettlementSnapshot createFrozenSnapshot(String contractorId, String periodId, BigDecimal totalHours, BigDecimal grossAmount, BigDecimal netAmount) {
        WorkforceSettlementSnapshot snapshot = repository.findByContractorIdAndPeriodId(contractorId, periodId)
                .orElseGet(() -> new WorkforceSettlementSnapshot(contractorId, periodId, totalHours, grossAmount, netAmount));
        return repository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public WorkforceSettlementSnapshot getSnapshot(String contractorId, String periodId) {
        return repository.findByContractorIdAndPeriodId(contractorId, periodId)
                .orElseThrow(() -> new BusinessRuleException("Workforce settlement snapshot not found", "SETTLEMENT_SNAPSHOT_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
