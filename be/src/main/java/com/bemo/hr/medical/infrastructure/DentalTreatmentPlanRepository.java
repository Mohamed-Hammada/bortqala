package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.DentalTreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalTreatmentPlanRepository extends JpaRepository<DentalTreatmentPlan, String> {

    Optional<DentalTreatmentPlan> findByAppIdAndId(String appId, String id);

    List<DentalTreatmentPlan> findAllByAppIdAndPatientIdOrderByCreatedAtDesc(String appId, String patientId);
}
