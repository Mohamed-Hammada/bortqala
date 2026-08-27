package com.bemo.hr.recruitment.api;

import com.bemo.hr.recruitment.domain.ApplicationStage;
import com.bemo.hr.recruitment.domain.OpeningStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class RecruitmentApi {

    private RecruitmentApi() {
    }

    // ---- Job Opening ----

    public record CreateOpeningRequest(
            @NotBlank String titleAr,
            @NotBlank String titleEn,
            String departmentId,
            @NotNull @Min(1) Integer headcount,
            String description
    ) {
    }

    public record UpdateOpeningRequest(
            @NotBlank String titleAr,
            @NotBlank String titleEn,
            String departmentId,
            @NotNull @Min(1) Integer headcount,
            String description,
            boolean published
    ) {
    }

    public record OpeningResponse(
            String id,
            String titleAr,
            String titleEn,
            String departmentId,
            int headcount,
            OpeningStatus status,
            String description,
            boolean published,
            int applicationCount,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }

    // ---- Job Application ----

    public record CreateApplicationRequest(
            @NotBlank String openingId,
            @NotBlank String fullName,
            String phone,
            String email,
            String source,
            String cvAttachmentId
    ) {
    }

    public record ApplicationResponse(
            String id,
            String openingId,
            String fullName,
            String phone,
            String email,
            String source,
            String cvAttachmentId,
            ApplicationStage stage,
            Integer rating,
            String notes,
            String convertedEmployeeId,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }

    public record MoveStageRequest(
            @NotNull ApplicationStage toStage,
            String note
    ) {
    }

    public record ConvertToEmployeeRequest(
            String categoryId,
            String departmentId
    ) {
    }

    public record ConvertResponse(
            String employeeId,
            String applicationId
    ) {
    }

    public record DuplicateWarning(
            String applicationId,
            String fullName,
            String matchedBy
    ) {
    }

    // ---- Stage Event ----

    public record StageEventResponse(
            String id,
            ApplicationStage fromStage,
            ApplicationStage toStage,
            String actor,
            String note,
            long eventAt
    ) {
    }
}
