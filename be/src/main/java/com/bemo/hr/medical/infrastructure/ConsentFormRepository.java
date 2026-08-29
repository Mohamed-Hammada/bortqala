package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.ConsentForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentFormRepository extends JpaRepository<ConsentForm, String> {

    List<ConsentForm> findByAppIdAndPatientIdOrderBySignedAtDesc(String appId, String patientId);

    Optional<ConsentForm> findByAppIdAndId(String appId, String id);
}
