package com.bemo.hr.manufacturing.quality.infrastructure;

import com.bemo.hr.manufacturing.quality.domain.QualityPlanHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityPlanHeaderRepository extends JpaRepository<QualityPlanHeader, String> {
    List<QualityPlanHeader> findByItemId(String itemId);
}
