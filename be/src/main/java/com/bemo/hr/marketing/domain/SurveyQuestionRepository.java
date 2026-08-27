package com.bemo.hr.marketing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, String> {
    List<SurveyQuestion> findBySurveyIdOrderBySortOrderAsc(String surveyId);
}
