package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.HospitalAdmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalAdmissionRepository extends JpaRepository<HospitalAdmission, String> {

    Optional<HospitalAdmission> findByAppIdAndId(String appId, String id);

    Optional<HospitalAdmission> findByAppIdAndPatientIdAndStatus(String appId, String patientId, HospitalAdmission.Status status);

    List<HospitalAdmission> findAllByAppIdAndStatusOrderByAdmittedAtDesc(String appId, HospitalAdmission.Status status);

    List<HospitalAdmission> findAllByAppIdAndPatientIdOrderByAdmittedAtDesc(String appId, String patientId);

    List<HospitalAdmission> findAllByAppIdOrderByAdmittedAtDesc(String appId);
}
