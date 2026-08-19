package com.bemo.hr.project.executive.infrastructure;

import com.bemo.hr.project.executive.domain.ProjectExecutiveSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectExecutiveSnapshotRepository extends JpaRepository<ProjectExecutiveSnapshot, String> {

    List<ProjectExecutiveSnapshot> findTop30ByOrderBySnapshotDateDesc();

    Optional<ProjectExecutiveSnapshot> findBySnapshotDate(LocalDate snapshotDate);
}
