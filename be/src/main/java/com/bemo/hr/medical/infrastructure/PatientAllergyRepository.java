package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, String> {

    List<PatientAllergy> findByAppIdAndPatientIdOrderByNotedAtDesc(String appId, String patientId);

    Optional<PatientAllergy> findByAppIdAndId(String appId, String id);

    void deleteByAppIdAndId(String appId, String id);
}
