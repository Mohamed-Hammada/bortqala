package com.bemo.hr.helpdesk.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HelpdeskCategoryRepository extends JpaRepository<HelpdeskCategory, String> {
    List<HelpdeskCategory> findByAppIdAndActiveTrueOrderByCreatedAtDesc(String appId);
    List<HelpdeskCategory> findByAppIdOrderByCreatedAtDesc(String appId);
}
