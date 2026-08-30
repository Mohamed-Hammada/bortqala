package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientDocumentRepository extends JpaRepository<PatientDocument, String> {

    List<PatientDocument> findByAppIdAndPatientIdOrderByUploadedAtDesc(String appId, String patientId);

    Optional<PatientDocument> findByAppIdAndId(String appId, String id);

    void deleteByAppIdAndId(String appId, String id);
}
