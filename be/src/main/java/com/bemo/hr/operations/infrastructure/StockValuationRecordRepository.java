package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockValuationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockValuationRecordRepository extends JpaRepository<StockValuationRecord, String> {
    List<StockValuationRecord> findByAsOfDate(LocalDate asOfDate);
}
