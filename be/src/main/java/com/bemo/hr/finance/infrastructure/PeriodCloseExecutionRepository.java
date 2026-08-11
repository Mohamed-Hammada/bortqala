package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.close.PeriodCloseExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeriodCloseExecutionRepository extends JpaRepository<PeriodCloseExecutionRecord, String> {
    List<PeriodCloseExecutionRecord> findByPeriodId(String periodId);
}
