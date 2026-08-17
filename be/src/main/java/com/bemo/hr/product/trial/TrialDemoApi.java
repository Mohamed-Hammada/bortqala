package com.bemo.hr.product.trial;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class TrialDemoApi {
    private TrialDemoApi() {
    }

    public record StartRequest(@Min(1) @Max(90) int days, boolean demo, String templateCode, Integer templateVersion,
                               @NotBlank String operationId) {
    }

    public record ConvertRequest(@NotBlank String operationId) {
    }

    public record ResetRequest(@NotBlank String operationId, Integer templateVersion) {
    }

    public record TemplateResponse(String code, int version, String nameKey) {
    }

    public record SampleResponse(String key, String payloadJson) {
    }

    public record StatusResponse(String tenantId, String commercialState, long trialStartedAt, long trialEndsAt,
                                 long convertedAt,
                                 boolean writeAllowed, boolean demoTenant, String templateCode, Integer templateVersion,
                                 long lastResetAt, String lastResetBy, int sampleCount, List<SampleResponse> samples) {
    }
}
