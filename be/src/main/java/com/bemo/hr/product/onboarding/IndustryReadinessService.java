package com.bemo.hr.product.onboarding;

import com.bemo.hr.product.pack.IndustryOnboardingStep;
import com.bemo.hr.product.pack.TenantIndustryPack;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryReadinessService {
    private final OnboardingEvidenceRegistry evidenceRegistry;

    public ReadinessEvaluation evaluate(String packCode, TenantIndustryPack pack, List<IndustryOnboardingStep> steps) {
        List<GuidedOnboardingApi.IssueResponse> issues = new ArrayList<>();
        int mandatoryChecksCount = 0;
        int passedChecksCount = 0;

        for (IndustryOnboardingStep step : steps) {
            var evidenceOpt = evidenceRegistry.evaluate(packCode, step.getStepKey());
            if (evidenceOpt.isPresent()) {
                var evidence = evidenceOpt.get();
                if (!step.isOptional()) {
                    mandatoryChecksCount++;
                    if (evidence.satisfied()) {
                        passedChecksCount++;
                    }
                }
                if (!evidence.satisfied() && evidence.blocker()) {
                    issues.add(new GuidedOnboardingApi.IssueResponse(
                            evidence.issueCode(),
                            evidence.labelKey(),
                            evidence.remediationRoute(),
                            evidence.count(),
                            evidence.blocker()
                    ));
                }
            }
        }

        int qualityScore = mandatoryChecksCount == 0 ? 100 : (passedChecksCount * 100 / mandatoryChecksCount);

        long requiredSteps = steps.stream().filter(s -> !s.isOptional()).count();
        long completedSteps = steps.stream().filter(s -> !s.isOptional() && (s.getStatus() == IndustryOnboardingStep.Status.COMPLETED || s.getStatus() == IndustryOnboardingStep.Status.SKIPPED)).count();
        int setupProgress = requiredSteps == 0 ? 100 : (int) (completedSteps * 100 / requiredSteps);

        boolean hasBlockers = issues.stream().anyMatch(GuidedOnboardingApi.IssueResponse::blocker);
        String readiness = (setupProgress == 100 && qualityScore >= 80 && !hasBlockers)
                ? "READY"
                : hasBlockers ? "BLOCKED" : "IN_PROGRESS";

        return new ReadinessEvaluation(setupProgress, qualityScore, readiness, List.copyOf(issues));
    }

    public boolean isGoLiveReady(String packCode, TenantIndustryPack pack, List<IndustryOnboardingStep> steps) {
        if (pack == null || steps.isEmpty()) {
            return false;
        }
        return "READY".equals(evaluate(packCode, pack, steps).readiness());
    }

    public record ReadinessEvaluation(
            int setupProgress,
            int dataQualityScore,
            String readiness,
            List<GuidedOnboardingApi.IssueResponse> issues
    ) {}
}
