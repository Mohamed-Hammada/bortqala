package com.bemo.hr.marketing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

@Entity
@Table(name = "survey_questions")
public class SurveyQuestion {

    public enum QuestionType { CHOICE, MULTI, RATING, TEXT }

    @Id
    private String id;
    @TenantId
    @Column(name = "app_id", nullable = false, length = 50)
    private String appId;
    @Column(name = "survey_id", nullable = false, length = 36)
    private String surveyId;
    @Column(nullable = false, length = 500)
    private String questionText;
    @Column(name = "question_type", nullable = false, length = 20)
    private String questionType;
    @Column(name = "options", length = 2000)
    private String options;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "created_at", nullable = false)
    private long createdAt;

    protected SurveyQuestion() {}

    public SurveyQuestion(String appId, String surveyId, String questionText,
                          QuestionType type, String options, int sortOrder, boolean required) {
        this.id = UUID.randomUUID().toString();
        this.appId = appId;
        this.surveyId = surveyId;
        this.questionText = questionText;
        this.questionType = type.name();
        this.options = options;
        this.sortOrder = sortOrder;
        this.required = required;
    }

    @PrePersist
    void prePersist() { createdAt = System.currentTimeMillis(); }

    public String getId() { return id; }
    public String getSurveyId() { return surveyId; }
    public String getQuestionText() { return questionText; }
    public QuestionType getQuestionType() { return QuestionType.valueOf(questionType); }
    public String getOptions() { return options; }
    public int getSortOrder() { return sortOrder; }
    public boolean isRequired() { return required; }
    public String getQuestionTypeStr() { return questionType; }
    public void setQuestionText(String v) { this.questionText = v; }
    public void setOptions(String v) { this.options = v; }
}
