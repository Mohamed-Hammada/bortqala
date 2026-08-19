package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ScheduleBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleBaselineRepository extends JpaRepository<ScheduleBaseline, String> {

    List<ScheduleBaseline> findByScheduleIdOrderByVersionNumberDesc(String scheduleId);

    Optional<ScheduleBaseline> findByScheduleIdAndVersionNumber(String scheduleId, int versionNumber);
}
