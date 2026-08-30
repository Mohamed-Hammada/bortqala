package com.bemo.hr.performance.infrastructure;

import com.bemo.hr.performance.domain.PerformanceAppraisal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceAppraisalRepository extends JpaRepository<PerformanceAppraisal, String> {

    List<PerformanceAppraisal> findByCycleIdOrderByCreatedAtDesc(String cycleId);

    List<PerformanceAppraisal> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    List<PerformanceAppraisal> findAllByOrderByCreatedAtDesc();

    Optional<PerformanceAppraisal> findByCycleIdAndEmployeeId(String cycleId, String employeeId);
}
