package com.bemo.hr.workforce.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.workforce.domain.WorkforceAttendanceLock;
import com.bemo.hr.workforce.infrastructure.WorkforceAttendanceLockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class WorkforceAttendanceLockService {

    private final WorkforceAttendanceLockRepository repository;

    public WorkforceAttendanceLockService(WorkforceAttendanceLockRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WorkforceAttendanceLock lockAttendance(String contractorId, String periodId, BigDecimal totalHours, String lockedBy) {
        WorkforceAttendanceLock lock = repository.findByContractorIdAndPeriodId(contractorId, periodId)
                .orElseGet(() -> new WorkforceAttendanceLock(contractorId, periodId, totalHours, lockedBy));
        return repository.save(lock);
    }

    @Transactional
    public WorkforceAttendanceLock correctLock(String id, BigDecimal newTotalHours, String reason) {
        WorkforceAttendanceLock lock = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Workforce attendance lock not found", "ATTENDANCE_LOCK_NOT_FOUND", HttpStatus.NOT_FOUND));
        lock.correct(newTotalHours, reason);
        return repository.save(lock);
    }

    @Transactional(readOnly = true)
    public List<WorkforceAttendanceLock> getLocksForContractor(String contractorId) {
        return repository.findByContractorId(contractorId);
    }
}
