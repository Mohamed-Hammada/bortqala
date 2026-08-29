package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.InsurancePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, String> {

    Optional<InsurancePlan> findByAppIdAndId(String appId, String id);

    List<InsurancePlan> findAllByAppIdAndPayerIdOrderByNameAsc(String appId, String payerId);

    List<InsurancePlan> findAllByAppIdOrderByNameAsc(String appId);
}
