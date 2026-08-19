package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ScheduleBaselineTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleBaselineTaskRepository extends JpaRepository<ScheduleBaselineTask, String> {

    List<ScheduleBaselineTask> findByBaselineId(String baselineId);
}
