package com.bemo.hr.marketing.application;

import com.bemo.hr.marketing.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketingService {

    private final CampaignRepository campaignRepo;
    private final CampaignRecipientRepository recipientRepo;
    private final SurveyRepository surveyRepo;
    private final SurveyQuestionRepository questionRepo;
    private final SurveyResponseRepository responseRepo;

    @Transactional
    public Campaign createCampaign(String appId, String name, String channel,
                                   String subject, String bodyAr, String bodyEn,
                                   String segmentSnapshot) {
        Campaign c = new Campaign(appId, name, Campaign.Channel.valueOf(channel.toUpperCase()),
                subject, bodyAr, bodyEn, segmentSnapshot);
        return campaignRepo.save(c);
    }

    @Transactional(readOnly = true)
    public List<Campaign> listCampaigns(String appId) {
        return campaignRepo.findByAppIdOrderByCreatedAtDesc(appId);
    }

    @Transactional(readOnly = true)
    public Campaign getCampaign(String appId, String campaignId) {
        Campaign c = campaignRepo.findById(campaignId)
                .filter(x -> x.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Campaign not found.",
                        "CAMPAIGN_NOT_FOUND", HttpStatus.NOT_FOUND));
        return c;
    }

    @Transactional
    public Campaign sendCampaign(String appId, String campaignId) {
        Campaign c = getCampaign(appId, campaignId);
        List<CampaignRecipient> queued = recipientRepo.findByCampaignIdAndStatus(campaignId,
                CampaignRecipient.Status.QUEUED.name());
        c.startSending(queued.size());
        campaignRepo.save(c);

        String bodyTemplate = "ar".equalsIgnoreCase(resolveDefaultLocale())
                ? c.getBodyAr() : c.getBodyEn();
        int sent = 0, failed = 0;
        for (CampaignRecipient r : queued) {
            try {
                r.markSent(System.currentTimeMillis());
                c.incrementSent();
                sent++;
            } catch (Exception ex) {
                r.markFailed(ex.getMessage());
                c.incrementFailed();
                failed++;
            }
            recipientRepo.save(r);
        }
        if (queued.isEmpty()) {
            c.markSent();
        } else if (failed == queued.size()) {
            c.markFailed("All recipients failed");
        } else if (c.getSentCount() + c.getFailedCount() >= c.getTotalRecipients()) {
            c.markSent();
        }
        campaignRepo.save(c);
        return c;
    }

    @Transactional
    public void addRecipients(String appId, String campaignId, List<RecipientDto> recipients) {
        getCampaign(appId, campaignId);
        for (RecipientDto r : recipients) {
            CampaignRecipient cr = new CampaignRecipient(appId, campaignId, r.targetRef(),
                    r.email(), r.phone(), r.locale());
            recipientRepo.save(cr);
        }
    }

    @Transactional(readOnly = true)
    public List<CampaignRecipient> listRecipients(String appId, String campaignId) {
        getCampaign(appId, campaignId);
        return recipientRepo.findByCampaignIdOrderByCreatedAtAsc(campaignId);
    }

    public record RecipientDto(String targetRef, String email, String phone, String locale) {}

    @Transactional
    public Campaign abortCampaign(String appId, String campaignId) {
        Campaign c = getCampaign(appId, campaignId);
        c.markFailed("Aborted by user");
        return campaignRepo.save(c);
    }

    // ---- Surveys ----

    @Transactional
    public Survey createSurvey(String appId, String title, String description) {
        return surveyRepo.save(new Survey(appId, title, description));
    }

    @Transactional(readOnly = true)
    public List<Survey> listSurveys(String appId) {
        return surveyRepo.findByAppIdOrderByCreatedAtDesc(appId);
    }

    @Transactional(readOnly = true)
    public Survey getSurvey(String appId, String surveyId) {
        return surveyRepo.findByAppIdAndId(appId, surveyId)
                .orElseThrow(() -> new BusinessRuleException("Survey not found.",
                        "SURVEY_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public SurveyQuestion addQuestion(String appId, String surveyId, String text,
                                      String type, String options, int order, boolean required) {
        getSurvey(appId, surveyId);
        return questionRepo.save(new SurveyQuestion(appId, surveyId, text,
                SurveyQuestion.QuestionType.valueOf(type.toUpperCase()), options, order, required));
    }

    @Transactional(readOnly = true)
    public List<SurveyQuestion> listQuestions(String appId, String surveyId) {
        getSurvey(appId, surveyId);
        return questionRepo.findBySurveyIdOrderBySortOrderAsc(surveyId);
    }

    @Transactional
    public void submitResponse(String appId, String surveyId, String respondentToken,
                               List<ResponseAnswerDto> answers) {
        Survey s = getSurvey(appId, surveyId);
        if (!s.isActive()) throw new BusinessRuleException("Survey is not active.",
                "SURVEY_INACTIVE", HttpStatus.BAD_REQUEST);
        if (responseRepo.existsBySurveyIdAndRespondentToken(surveyId, respondentToken))
            throw new BusinessRuleException("Duplicate response from this token.",
                    "SURVEY_DUPLICATE_RESPONSE", HttpStatus.CONFLICT);
        for (ResponseAnswerDto a : answers) {
            responseRepo.save(new SurveyResponse(appId, surveyId, a.questionId(), respondentToken, a.answer()));
        }
    }

    public record ResponseAnswerDto(String questionId, String answer) {}

    @Transactional(readOnly = true)
    public Map<String, Object> getResults(String appId, String surveyId) {
        getSurvey(appId, surveyId);
        List<SurveyQuestion> questions = questionRepo.findBySurveyIdOrderBySortOrderAsc(surveyId);
        List<SurveyResponse> allResponses = responseRepo.findBySurveyIdOrderByCreatedAtAsc(surveyId);
        long totalRespondents = allResponses.stream()
                .map(SurveyResponse::getRespondentToken).distinct().count();
        return Map.of("questions", questions, "responses", allResponses, "totalRespondents", totalRespondents);
    }

    private String resolveDefaultLocale() { return "ar-EG"; }
}
