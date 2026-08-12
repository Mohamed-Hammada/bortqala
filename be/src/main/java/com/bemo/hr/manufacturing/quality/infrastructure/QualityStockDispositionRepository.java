package com.bemo.hr.manufacturing.quality.infrastructure;

import com.bemo.hr.manufacturing.quality.domain.QualityStockDisposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityStockDispositionRepository extends JpaRepository<QualityStockDisposition, String> {
    List<QualityStockDisposition> findByInspectionId(String inspectionId);
}
