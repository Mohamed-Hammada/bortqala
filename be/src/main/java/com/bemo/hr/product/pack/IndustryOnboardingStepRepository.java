package com.bemo.hr.product.pack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndustryOnboardingStepRepository extends JpaRepository<IndustryOnboardingStep, String> {
    List<IndustryOnboardingStep> findByTenantPackIdOrderBySequenceNo(String packId);

    Optional<IndustryOnboardingStep> findByTenantPackIdAndStepKey(String packId, String key);
}
