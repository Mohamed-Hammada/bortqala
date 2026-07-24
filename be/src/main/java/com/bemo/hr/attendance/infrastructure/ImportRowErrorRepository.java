package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.domain.ImportRowError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportRowErrorRepository extends JpaRepository<ImportRowError, String> {
    List<ImportRowError> findByBatchIdOrderByRowNumber(String batchId);
}
