package com.bemo.hr.compliance.privacy.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivacyRequestRepository extends JpaRepository<PrivacyRequest, String> {
    List<PrivacyRequest> findByAppIdOrderByCreatedAtDesc(String appId);
    List<PrivacyRequest> findByAppIdAndStatusIn(String appId, List<String> statuses);
    long countByAppIdAndSubjectRef(String appId, String subjectRef);
}
