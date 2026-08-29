package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PatientInsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientInsurancePolicyRepository extends JpaRepository<PatientInsurancePolicy, String> {

    Optional<PatientInsurancePolicy> findByAppIdAndId(String appId, String id);

    List<PatientInsurancePolicy> findAllByAppIdAndPatientIdOrderByCreatedAtDesc(String appId, String patientId);

    Optional<PatientInsurancePolicy> findByAppIdAndPatientIdAndIsPrimaryTrue(String appId, String patientId);
}
