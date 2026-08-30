package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PharmacyDispenseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyDispenseRecordRepository extends JpaRepository<PharmacyDispenseRecord, String> {

    Optional<PharmacyDispenseRecord> findByAppIdAndId(String appId, String id);

    List<PharmacyDispenseRecord> findAllByAppIdAndPrescriptionId(String appId, String prescriptionId);

    List<PharmacyDispenseRecord> findAllByAppIdAndPatientIdOrderByCreatedAtDesc(String appId, String patientId);

    List<PharmacyDispenseRecord> findAllByAppIdAndStatusOrderByCreatedAtDesc(String appId, PharmacyDispenseRecord.Status status);

    List<PharmacyDispenseRecord> findAllByAppIdOrderByCreatedAtDesc(String appId);
}
