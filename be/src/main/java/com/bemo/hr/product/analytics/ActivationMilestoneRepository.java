package com.bemo.hr.product.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivationMilestoneRepository extends JpaRepository<ActivationMilestone, String> {
    boolean existsByMilestoneKey(String key);

    List<ActivationMilestone> findAllByOrderByAchievedAtAsc();
}
