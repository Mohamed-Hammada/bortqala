package com.bemo.hr.product.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingAssessmentRepository extends JpaRepository<OnboardingAssessment, String> {
    Optional<OnboardingAssessment> findByTenantPackIdAndOperationId(String tenantPackId, String operationId);

    Optional<OnboardingAssessment> findFirstByTenantPackIdOrderByAssessedAtDesc(String tenantPackId);

    Optional<OnboardingAssessment> findFirstByOrderByAssessedAtDesc();
}
