package com.bemo.hr.recruitment.infrastructure;

import com.bemo.hr.recruitment.domain.ApplicationStageEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStageEventRepository extends JpaRepository<ApplicationStageEvent, String> {
    List<ApplicationStageEvent> findByApplicationIdOrderByEventAtAsc(String applicationId);
}
