package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.SiteCustody;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteCustodyRepository extends JpaRepository<SiteCustody, String> {
    List<SiteCustody> findByProjectIdOrderByIssuedAtDesc(String projectId);
    Optional<SiteCustody> findByIdAndProjectId(String id, String projectId);
    List<SiteCustody> findByCustodianEmployeeId(String employeeId);
}
