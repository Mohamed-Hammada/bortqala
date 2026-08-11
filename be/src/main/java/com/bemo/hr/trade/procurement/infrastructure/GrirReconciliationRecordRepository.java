package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.GrirReconciliationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrirReconciliationRecordRepository extends JpaRepository<GrirReconciliationRecord, String> {
    List<GrirReconciliationRecord> findByStatus(GrirReconciliationRecord.Status status);
}
