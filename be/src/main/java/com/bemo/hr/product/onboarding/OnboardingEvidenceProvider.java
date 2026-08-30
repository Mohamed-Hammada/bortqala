package com.bemo.hr.product.onboarding;

public interface OnboardingEvidenceProvider {
    boolean supports(String packCode, String stepKey);

    EvidenceResult evaluate(String packCode, String stepKey);

    record EvidenceResult(
            boolean satisfied,
            long count,
            boolean blocker,
            String issueCode,
            String labelKey,
            String remediationRoute
    ) {}
}
