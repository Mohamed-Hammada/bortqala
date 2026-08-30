package com.bemo.hr.marketing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "survey_responses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"app_id", "survey_id", "respondent_token"}))
public class SurveyResponse {

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "survey_id", nullable = false, length = 36)
    private String surveyId;
    @Column(name = "question_id", nullable = false, length = 36)
    private String questionId;
    @Column(name = "respondent_token", nullable = false, length = 100)
    private String respondentToken;
    @Column(length = 4000)
    private String answer;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected SurveyResponse() {}

    public SurveyResponse(String appId, String surveyId, String questionId,
                          String respondentToken, String answer) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.surveyId = surveyId;
        this.questionId = questionId;
        this.respondentToken = respondentToken;
        this.answer = answer;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getSurveyId() { return surveyId; }
    public String getQuestionId() { return questionId; }
    public String getRespondentToken() { return respondentToken; }
    public String getAnswer() { return answer; }
    public long getCreatedAt() { return createdAt; }
}
