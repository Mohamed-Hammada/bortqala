package com.bemo.hr.product.subscription;

import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.Set;

public final class SubscriptionApi {
    private SubscriptionApi() {
    }

    public record PlanResponse(String code, String nameAr, String nameEn, Set<String> features,
                               Map<String, Integer> limits, boolean active, long version) {
    }

    public record PlanUpsertRequest(@NotBlank @Size(max = 120) String nameAr, @NotBlank @Size(max = 120) String nameEn,
                                    @NotEmpty Set<@NotBlank String> featureKeys,
                                    @NotNull Map<@NotBlank String, @PositiveOrZero Integer> limits, boolean active,
                                    long expectedVersion) {
    }

    public record SubscriptionResponse(String planCode, String status, long startsAt, long renewsAt, long endsAt,
                                       long version, String updatedBy, long updatedAt) {
    }

    public record ChangeRequest(@NotBlank @Size(max = 40) String planCode,
                                @NotBlank @Pattern(regexp = "ACTIVE|TRIAL|PAST_DUE|CANCELED") String status,
                                @Positive long startsAt, @PositiveOrZero long renewsAt, @PositiveOrZero long endsAt,
                                @NotBlank @Size(max = 500) String reason, @NotBlank @Size(max = 80) String operationId,
                                long expectedVersion) {
    }

    public record ChangeResponse(SubscriptionResponse subscription, boolean replayed) {
    }

    public record HistoryResponse(String fromPlan, String toPlan, String fromStatus, String toStatus, String reason,
                                  String operationId, String actor, long changedAt) {
    }

    public record UsageResponse(String planCode, Map<String, Integer> limits, Map<String, Long> usage) {
    }
}
