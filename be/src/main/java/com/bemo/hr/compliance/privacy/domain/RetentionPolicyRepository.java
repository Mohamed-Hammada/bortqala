package com.bemo.hr.compliance.privacy.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, String> {
    List<RetentionPolicy> findByAppIdAndActiveTrue(String appId);
}
