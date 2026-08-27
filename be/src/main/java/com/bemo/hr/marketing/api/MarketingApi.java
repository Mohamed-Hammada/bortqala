package com.bemo.hr.marketing.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class MarketingApi {

    public record CampaignResponse(String id, String name, String channel, String subject,
                                    String bodyAr, String bodyEn, String segmentSnapshot,
                                    String status, Long scheduledAtEpochMs,
                                    int totalRecipients, int sentCount, int failedCount,
                                    String errorMessage, Long createdAtEpochMs, Long version) {}

    public record CreateCampaignPayload(
            @NotBlank String name, @NotBlank String channel, String subject,
            String bodyAr, String bodyEn, String segmentSnapshot) {}

    public record AddRecipientsPayload(List<RecipientPayload> recipients) {}

    public record RecipientPayload(String targetRef, String email, String phone, String locale) {}

    public record RecipientResponse(String id, String campaignId, String targetRef,
                                     String email, String phone, String locale,
                                     String status, String errorMessage, Long sentAtEpochMs) {}

    public record SurveyResponse(String id, String title, String description,
                                  boolean active, Long createdAtEpochMs, Long version) {}

    public record CreateSurveyPayload(@NotBlank String title, String description) {}

    public record SurveyQuestionResponse(String id, String surveyId, String questionText,
                                          String questionType, String options, int sortOrder,
                                          boolean required) {}

    public record AddQuestionPayload(
            @NotBlank String questionText, @NotBlank String questionType,
            String options, int sortOrder, boolean required) {}

    public record SubmitResponsePayload(String respondentToken, List<AnswerPayload> answers) {}

    public record AnswerPayload(@NotBlank String questionId, String answer) {}

    public record ResultsPayload(Map<String, Object> results) {}
}
