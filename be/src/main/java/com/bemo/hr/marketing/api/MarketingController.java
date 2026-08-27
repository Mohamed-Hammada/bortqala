package com.bemo.hr.marketing.api;

import com.bemo.hr.marketing.application.MarketingService;
import com.bemo.hr.marketing.domain.Campaign;
import com.bemo.hr.marketing.domain.CampaignRecipient;
import com.bemo.hr.marketing.domain.Survey;
import com.bemo.hr.marketing.domain.SurveyQuestion;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/marketing")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MarketingController {

    private final MarketingService service;

    private String resolveAppId(Authentication auth) {
        if (auth.getDetails() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)
            return jwt.getClaimAsString("appId");
        return TenantContext.require();
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')")
    public MarketingApi.CampaignResponse createCampaign(
            @Valid @RequestBody MarketingApi.CreateCampaignPayload p, Authentication auth) {
        return toCampaignResp(service.createCampaign(resolveAppId(auth), p.name(), p.channel(),
                p.subject(), p.bodyAr(), p.bodyEn(), p.segmentSnapshot()));
    }

    @GetMapping("/campaigns")
    public List<MarketingApi.CampaignResponse> listCampaigns(Authentication auth) {
        return service.listCampaigns(resolveAppId(auth)).stream()
                .map(this::toCampaignResp).toList();
    }

    @GetMapping("/campaigns/{id}")
    public MarketingApi.CampaignResponse getCampaign(@PathVariable String id, Authentication auth) {
        return toCampaignResp(service.getCampaign(resolveAppId(auth), id));
    }

    @PostMapping("/campaigns/{id}/send")
    public MarketingApi.CampaignResponse sendCampaign(@PathVariable String id, Authentication auth) {
        return toCampaignResp(service.sendCampaign(resolveAppId(auth), id));
    }

    @PostMapping("/campaigns/{id}/recipients")
    @ResponseStatus(HttpStatus.CREATED)
    public void addRecipients(@PathVariable String id,
                              @Valid @RequestBody MarketingApi.AddRecipientsPayload p,
                              Authentication auth) {
        String appId = resolveAppId(auth);
        List<MarketingService.RecipientDto> dtos = p.recipients().stream()
                .map(r -> new MarketingService.RecipientDto(r.targetRef(), r.email(), r.phone(), r.locale()))
                .toList();
        service.addRecipients(appId, id, dtos);
    }

    @GetMapping("/campaigns/{id}/recipients")
    public List<MarketingApi.RecipientResponse> listRecipients(@PathVariable String id, Authentication auth) {
        return service.listRecipients(resolveAppId(auth), id).stream()
                .map(this::toRecipientResp).toList();
    }

    @PostMapping("/campaigns/{id}/abort")
    public MarketingApi.CampaignResponse abortCampaign(@PathVariable String id, Authentication auth) {
        return toCampaignResp(service.abortCampaign(resolveAppId(auth), id));
    }

    @PostMapping("/surveys")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES_MANAGER')")
    public MarketingApi.SurveyResponse createSurvey(
            @Valid @RequestBody MarketingApi.CreateSurveyPayload p, Authentication auth) {
        return toSurveyResp(service.createSurvey(resolveAppId(auth), p.title(), p.description()));
    }

    @GetMapping("/surveys")
    public List<MarketingApi.SurveyResponse> listSurveys(Authentication auth) {
        return service.listSurveys(resolveAppId(auth)).stream().map(this::toSurveyResp).toList();
    }

    @GetMapping("/surveys/{id}")
    public MarketingApi.SurveyResponse getSurvey(@PathVariable String id, Authentication auth) {
        return toSurveyResp(service.getSurvey(resolveAppId(auth), id));
    }

    @PostMapping("/surveys/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public MarketingApi.SurveyQuestionResponse addQuestion(
            @PathVariable String id,
            @Valid @RequestBody MarketingApi.AddQuestionPayload p, Authentication auth) {
        return toQuestionResp(service.addQuestion(resolveAppId(auth), id, p.questionText(),
                p.questionType(), p.options(), p.sortOrder(), p.required()));
    }

    @GetMapping("/surveys/{id}/questions")
    public List<MarketingApi.SurveyQuestionResponse> listQuestions(@PathVariable String id, Authentication auth) {
        return service.listQuestions(resolveAppId(auth), id).stream()
                .map(this::toQuestionResp).toList();
    }

    @PostMapping("/surveys/{id}/respond")
    public void submitResponse(@PathVariable String id,
                               @Valid @RequestBody MarketingApi.SubmitResponsePayload p,
                               Authentication auth) {
        String appId = resolveAppId(auth);
        List<MarketingService.ResponseAnswerDto> dtos = p.answers().stream()
                .map(a -> new MarketingService.ResponseAnswerDto(a.questionId(), a.answer()))
                .toList();
        service.submitResponse(appId, id, p.respondentToken(), dtos);
    }

    @GetMapping("/surveys/{id}/results")
    public Map<String, Object> getResults(@PathVariable String id, Authentication auth) {
        return service.getResults(resolveAppId(auth), id);
    }

    private MarketingApi.CampaignResponse toCampaignResp(Campaign c) {
        return new MarketingApi.CampaignResponse(c.getId(), c.getName(), c.getChannel(),
                c.getSubject(), c.getBodyAr(), c.getBodyEn(), c.getSegmentSnapshot(),
                c.getStatus(), c.getScheduledAtEpochMs(),
                c.getTotalRecipients(), c.getSentCount(), c.getFailedCount(),
                c.getErrorMessage(), c.getCreatedAt(), c.getVersion());
    }

    private MarketingApi.RecipientResponse toRecipientResp(CampaignRecipient r) {
        return new MarketingApi.RecipientResponse(r.getId(), r.getCampaignId(), r.getTargetRef(),
                r.getEmail(), r.getPhone(), r.getLocale(), r.getStatus(),
                r.getErrorMessage(), r.getSentAtEpochMs());
    }

    private MarketingApi.SurveyResponse toSurveyResp(Survey s) {
        return new MarketingApi.SurveyResponse(s.getId(), s.getTitle(), s.getDescription(),
                s.isActive(), s.getCreatedAt(), s.getVersion());
    }

    private MarketingApi.SurveyQuestionResponse toQuestionResp(SurveyQuestion q) {
        return new MarketingApi.SurveyQuestionResponse(q.getId(), q.getSurveyId(),
                q.getQuestionText(), q.getQuestionTypeStr(), q.getOptions(),
                q.getSortOrder(), q.isRequired());
    }
}
