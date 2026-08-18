package com.bemo.hr.product.onboarding;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.product.pack.IndustryOnboardingStep;
import com.bemo.hr.product.pack.IndustryOnboardingStepRepository;
import com.bemo.hr.product.pack.IndustryPack;
import com.bemo.hr.product.pack.IndustryPackRepository;
import com.bemo.hr.product.pack.TenantIndustryPack;
import com.bemo.hr.product.pack.TenantIndustryPackRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuidedOnboardingService {
    private final IndustryPackRepository packRepository;
    private final TenantIndustryPackRepository tenantPackRepository;
    private final IndustryOnboardingStepRepository stepRepository;
    private final OnboardingAssessmentRepository assessmentRepository;
    private final OnboardingEvidenceRegistry evidenceRegistry;
    private final IndustryReadinessService readinessService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    private static BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }

    @Transactional(readOnly = true)
    public GuidedOnboardingApi.OverviewResponse overview(String code) {
        log.debug("overview called with code={}", code);
        var context = context(code, false);
        var evaluation = readinessService.evaluate(code, context.pack(), context.steps());
        return new GuidedOnboardingApi.OverviewResponse(
                code,
                evaluation.setupProgress(),
                evaluation.dataQualityScore(),
                evaluation.readiness(),
                0,
                evaluation.issues(),
                stepViews(context.steps())
        );
    }

    @Transactional
    public GuidedOnboardingApi.OverviewResponse assess(String code, GuidedOnboardingApi.AssessRequest request, String actor) {
        log.debug("assess called with code={}, operationId={}", code, request.operationId());
        var context = context(code, true);
        var replay = assessmentRepository.findByTenantPackIdAndOperationId(context.pack().getId(), request.operationId());
        if (replay.isPresent()) return fromSnapshot(code, replay.get(), context.steps());

        autoComplete(code, context.steps(), actor);
        var evaluation = readinessService.evaluate(code, context.pack(), context.steps());

        String issues;
        try {
            issues = objectMapper.writeValueAsString(evaluation.issues());
        } catch (Exception ex) {
            log.error("Failed to serialize onboarding assessment issues", ex);
            throw error("ONBOARDING_ASSESSMENT_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        var saved = assessmentRepository.save(new OnboardingAssessment(
                context.pack().getId(),
                request.operationId(),
                evaluation.setupProgress(),
                evaluation.dataQualityScore(),
                evaluation.readiness(),
                issues,
                actor
        ));
        auditService.record("ASSESS", "ONBOARDING", saved.getId(), actor,
                "{\"pack\":\"" + code + "\",\"progress\":" + evaluation.setupProgress() + ",\"quality\":" + evaluation.dataQualityScore() + ",\"readiness\":\"" + evaluation.readiness() + "\"}", null);

        return new GuidedOnboardingApi.OverviewResponse(
                code,
                evaluation.setupProgress(),
                evaluation.dataQualityScore(),
                evaluation.readiness(),
                saved.getAssessedAt().toEpochMilli(),
                evaluation.issues(),
                stepViews(context.steps())
        );
    }

    private void autoComplete(String code, List<IndustryOnboardingStep> steps, String actor) {
        Map<String, IndustryOnboardingStep> byKey = new HashMap<>();
        for (var step : steps) {
            byKey.put(step.getStepKey(), step);
            if (step.getStatus() == IndustryOnboardingStep.Status.BLOCKED && terminal(byKey.get(step.getPrerequisiteKey()))) {
                step.ready();
            }
            if (step.getStatus() == IndustryOnboardingStep.Status.READY) {
                var evidence = evidenceRegistry.evaluate(code, step.getStepKey());
                if (evidence.isPresent() && evidence.get().satisfied()) {
                    step.complete(actor, false);
                }
            }
            stepRepository.save(step);
        }
    }

    private boolean terminal(IndustryOnboardingStep step) {
        return step != null && (step.getStatus() == IndustryOnboardingStep.Status.COMPLETED || step.getStatus() == IndustryOnboardingStep.Status.SKIPPED);
    }

    private List<GuidedOnboardingApi.StepResponse> stepViews(List<IndustryOnboardingStep> steps) {
        return steps.stream().map(s -> new GuidedOnboardingApi.StepResponse(s.getStepKey(), s.getSequenceNo(), s.isOptional(), s.getStatus().name())).toList();
    }

    private GuidedOnboardingApi.OverviewResponse fromSnapshot(String code, OnboardingAssessment snapshot, List<IndustryOnboardingStep> steps) {
        try {
            var issues = List.of(objectMapper.readValue(snapshot.getIssuesJson(), GuidedOnboardingApi.IssueResponse[].class));
            return new GuidedOnboardingApi.OverviewResponse(
                    code,
                    snapshot.getSetupProgress(),
                    snapshot.getDataQualityScore(),
                    snapshot.getReadiness(),
                    snapshot.getAssessedAt().toEpochMilli(),
                    issues,
                    stepViews(steps)
            );
        } catch (Exception ex) {
            log.error("Failed to deserialize onboarding assessment snapshot for code={}", code, ex);
            throw error("ONBOARDING_ASSESSMENT_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Context context(String code, boolean lock) {
        IndustryPack definition = packRepository.findByCodeAndStatus(code, "ACTIVE")
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_FOUND", HttpStatus.NOT_FOUND));
        TenantIndustryPack pack = (lock ? tenantPackRepository.findByPackIdForUpdate(definition.getId()) : tenantPackRepository.findByPackId(definition.getId()))
                .orElseThrow(() -> error("INDUSTRY_PACK_NOT_INSTALLED", HttpStatus.CONFLICT));
        return new Context(pack, stepRepository.findByTenantPackIdOrderBySequenceNo(pack.getId()));
    }

    private record Context(TenantIndustryPack pack, List<IndustryOnboardingStep> steps) {}
}
