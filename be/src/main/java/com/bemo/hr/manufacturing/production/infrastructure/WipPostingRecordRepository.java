package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.WipPostingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WipPostingRecordRepository extends JpaRepository<WipPostingRecord, String> {
    List<WipPostingRecord> findByWorkOrderId(String workOrderId);
}
