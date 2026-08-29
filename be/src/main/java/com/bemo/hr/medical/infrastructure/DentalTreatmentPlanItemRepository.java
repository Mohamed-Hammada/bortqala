package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.DentalTreatmentPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalTreatmentPlanItemRepository extends JpaRepository<DentalTreatmentPlanItem, String> {

    Optional<DentalTreatmentPlanItem> findByAppIdAndId(String appId, String id);

    List<DentalTreatmentPlanItem> findAllByAppIdAndPlanIdOrderByToothNumberAsc(String appId, String planId);
}
