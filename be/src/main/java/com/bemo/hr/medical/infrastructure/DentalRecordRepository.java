package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.DentalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DentalRecordRepository extends JpaRepository<DentalRecord, String> {

    Optional<DentalRecord> findByAppIdAndId(String appId, String id);

    List<DentalRecord> findAllByAppIdAndPatientIdOrderByNotedOnDesc(String appId, String patientId);

    List<DentalRecord> findAllByAppIdAndPatientIdAndToothNumberOrderByNotedOnDesc(String appId, String patientId, Integer toothNumber);
}
