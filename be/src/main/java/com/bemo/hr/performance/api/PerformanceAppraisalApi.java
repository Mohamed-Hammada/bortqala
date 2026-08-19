package com.bemo.hr.performance.api;

import com.bemo.hr.performance.domain.AppraisalStatus;
import com.bemo.hr.performance.domain.CycleStatus;
import com.bemo.hr.performance.domain.KpiCategory;
import com.bemo.hr.performance.domain.RatingBand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PerformanceAppraisalApi {

    private PerformanceAppraisalApi() {
    }

    public record CreateCycleRequest(
            @NotBlank @Size(max = 200) String nameAr,
            @NotBlank @Size(max = 200) String nameEn,
            int periodYear,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {
    }

    public record PerformanceCycleResponse(
            String id,
            String nameAr,
            String nameEn,
            int periodYear,
            LocalDate startDate,
            LocalDate endDate,
            CycleStatus status,
            long createdAt
    ) {
    }

    public record CreateKpiRequest(
            @NotBlank String cycleId,
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 200) String titleAr,
            @NotBlank @Size(max = 200) String titleEn,
            KpiCategory category,
            BigDecimal targetValue,
            @NotNull BigDecimal weightPercentage
    ) {
    }

    public record PerformanceKpiResponse(
            String id,
            String cycleId,
            String code,
            String titleAr,
            String titleEn,
            KpiCategory category,
            BigDecimal targetValue,
            BigDecimal weightPercentage,
            long createdAt
    ) {
    }

    public record InitAppraisalRequest(
            @NotBlank String cycleId,
            @NotBlank String employeeId,
            String reviewerId
    ) {
    }

    public record KpiScoreInput(
            @NotBlank String kpiId,
            BigDecimal selfRating,
            BigDecimal managerRating,
            String comments
    ) {
    }

    public record SubmitAppraisalRequest(
            List<KpiScoreInput> kpiScores,
            String managerFeedback,
            String developmentPlan
    ) {
    }

    public record AppraisalKpiScoreResponse(
            String id,
            String kpiId,
            String kpiCode,
            String kpiTitleAr,
            String kpiTitleEn,
            KpiCategory category,
            BigDecimal weightPercentage,
            BigDecimal selfRating,
            BigDecimal managerRating,
            BigDecimal weightedScore,
            String comments
    ) {
    }

    public record PerformanceAppraisalResponse(
            String id,
            String cycleId,
            String cycleNameAr,
            String cycleNameEn,
            String employeeId,
            String employeeName,
            String employeeCode,
            String reviewerId,
            String reviewerName,
            BigDecimal selfScore,
            BigDecimal managerScore,
            BigDecimal finalScore,
            RatingBand ratingBand,
            AppraisalStatus status,
            String managerFeedback,
            String developmentPlan,
            List<AppraisalKpiScoreResponse> scores,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }
}
