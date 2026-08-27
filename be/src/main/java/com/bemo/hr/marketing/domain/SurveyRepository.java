package com.bemo.hr.marketing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, String> {
    List<Survey> findByAppIdOrderByCreatedAtDesc(String appId);
    Optional<Survey> findByAppIdAndId(String appId, String id);
}
