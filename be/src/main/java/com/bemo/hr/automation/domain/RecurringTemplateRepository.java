package com.bemo.hr.automation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringTemplateRepository extends JpaRepository<RecurringTemplate, String> {
    List<RecurringTemplate> findByAppIdAndActiveTrue(String appId);
    List<RecurringTemplate> findByAppIdOrderByCreatedAtDesc(String appId);
}
