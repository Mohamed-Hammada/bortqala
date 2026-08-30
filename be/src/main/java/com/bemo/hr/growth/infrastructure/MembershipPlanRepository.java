package com.bemo.hr.growth.infrastructure;

import com.bemo.hr.growth.domain.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, String> {
    List<MembershipPlan> findByAppIdAndActiveTrue(String appId);
}
