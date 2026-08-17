package com.bemo.hr.workforce.infrastructure;

import com.bemo.hr.workforce.domain.WorkforceAttendanceLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkforceAttendanceLockRepository extends JpaRepository<WorkforceAttendanceLock, String> {
    Optional<WorkforceAttendanceLock> findByContractorIdAndPeriodId(String contractorId, String periodId);

    List<WorkforceAttendanceLock> findByContractorId(String contractorId);
}
