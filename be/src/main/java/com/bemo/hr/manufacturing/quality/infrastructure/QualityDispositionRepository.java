package com.bemo.hr.manufacturing.quality.infrastructure;

import com.bemo.hr.manufacturing.quality.domain.QualityDisposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityDispositionRepository extends JpaRepository<QualityDisposition, String> {
    List<QualityDisposition> findByPlanId(String planId);
}
