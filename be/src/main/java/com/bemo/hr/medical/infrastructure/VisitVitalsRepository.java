package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.VisitVitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitVitalsRepository extends JpaRepository<VisitVitals, String> {

    List<VisitVitals> findByAppIdAndPatientIdOrderByRecordedAtDesc(String appId, String patientId);

    Optional<VisitVitals> findByAppIdAndVisitId(String appId, String visitId);

    Optional<VisitVitals> findByAppIdAndId(String appId, String id);
}
