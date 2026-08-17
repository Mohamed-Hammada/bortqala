package com.bemo.hr.reporting.infrastructure;

import com.bemo.hr.reporting.domain.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, String> {
    List<AttendancePolicy> findAllByOrderByPriorityDescEffectiveFromDesc();
}
