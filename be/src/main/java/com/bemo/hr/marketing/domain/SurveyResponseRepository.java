package com.bemo.hr.marketing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, String> {
    List<SurveyResponse> findBySurveyIdAndRespondentTokenOrderByCreatedAtAsc(String surveyId, String respondentToken);
    List<SurveyResponse> findBySurveyIdOrderByCreatedAtAsc(String surveyId);
    boolean existsBySurveyIdAndRespondentToken(String surveyId, String respondentToken);
}
