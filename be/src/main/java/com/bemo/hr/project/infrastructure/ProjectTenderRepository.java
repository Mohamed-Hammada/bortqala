package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProjectTender;
import com.bemo.hr.project.domain.TenderStatus;
import com.bemo.hr.project.domain.TenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectTenderRepository extends JpaRepository<ProjectTender, String> {

    List<ProjectTender> findAllByOrderByCreatedAtDesc();

    List<ProjectTender> findByStatusOrderByCreatedAtDesc(TenderStatus status);

    List<ProjectTender> findByTenderTypeOrderByCreatedAtDesc(TenderType tenderType);

    List<ProjectTender> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<ProjectTender> findByTenderNumber(String tenderNumber);

    @Query("SELECT COUNT(t) FROM ProjectTender t WHERE t.tenderNumber LIKE CONCAT('TND-', :yearPrefix, '-%')")
    long countByYearPrefix(@Param("yearPrefix") String yearPrefix);
}
