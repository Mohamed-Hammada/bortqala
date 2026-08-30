package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.InsurancePreAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsurancePreAuthorizationRepository extends JpaRepository<InsurancePreAuthorization, String> {

    Optional<InsurancePreAuthorization> findByAppIdAndId(String appId, String id);

    Optional<InsurancePreAuthorization> findByAppIdAndApprovalCode(String appId, String approvalCode);

    List<InsurancePreAuthorization> findAllByAppIdAndPatientIdOrderByCreatedAtDesc(String appId, String patientId);

    List<InsurancePreAuthorization> findAllByAppIdOrderByCreatedAtDesc(String appId);

    List<InsurancePreAuthorization> findAllByAppIdAndStatusOrderByCreatedAtDesc(String appId, InsurancePreAuthorization.Status status);
}
