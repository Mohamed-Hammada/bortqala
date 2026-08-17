package com.bemo.hr.product.onboarding;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class GuidedOnboardingApi {
    private GuidedOnboardingApi() {
    }

    public record AssessRequest(@NotBlank String operationId) {
    }

    public record IssueResponse(String code, String labelKey, String route, long count, boolean blocker) {
    }

    public record StepResponse(String key, int sequence, boolean optional, String status) {
    }

    public record OverviewResponse(String packCode, int setupProgress, int dataQualityScore, String readiness,
                                   long assessedAt, List<IssueResponse> issues, List<StepResponse> steps) {
    }
}
