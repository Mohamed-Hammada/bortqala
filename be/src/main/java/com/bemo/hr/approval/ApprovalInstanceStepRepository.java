package com.bemo.hr.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalInstanceStepRepository extends JpaRepository<ApprovalInstanceStep, String> {
    List<ApprovalInstanceStep> findByInstanceIdOrderByStepOrderAsc(String instanceId);

    Optional<ApprovalInstanceStep> findByInstanceIdAndStepOrder(String instanceId, int stepOrder);
}
