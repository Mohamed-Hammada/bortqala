package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PatientFamilyLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientFamilyLinkRepository extends JpaRepository<PatientFamilyLink, String> {
    List<PatientFamilyLink> findByPatientId(String patientId);
    List<PatientFamilyLink> findByGuardianPatientId(String guardianPatientId);
    Optional<PatientFamilyLink> findByPatientIdAndGuardianPatientId(String patientId, String guardianPatientId);
}
