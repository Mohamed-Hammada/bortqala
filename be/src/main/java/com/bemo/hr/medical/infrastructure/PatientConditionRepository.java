package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PatientCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientConditionRepository extends JpaRepository<PatientCondition, String> {

    List<PatientCondition> findByAppIdAndPatientIdOrderByCreatedAtDesc(String appId, String patientId);

    Optional<PatientCondition> findByAppIdAndId(String appId, String id);

    void deleteByAppIdAndId(String appId, String id);
}
