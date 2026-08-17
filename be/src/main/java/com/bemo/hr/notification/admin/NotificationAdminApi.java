package com.bemo.hr.notification.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class NotificationAdminApi {
    private NotificationAdminApi() {
    }

    public record AppSummary(String id, String code, String name) {
    }

    public record UserSummary(String username, String displayName, boolean active) {
    }

    public record ExcelPreview(
            int totalRows, int validCount, int duplicateCount, int notFoundCount, int inactiveCount,
            List<String> validUsernames, List<String> duplicateUsernames,
            List<String> notFoundUsernames, List<String> inactiveUsernames) {
    }

    public record BulkSendPayload(
            @NotBlank @Size(max = 36) String targetAppId,
            @NotBlank @Pattern(regexp = "USERS|EXCEL|APP") String mode,
            @Size(max = 5000) List<@NotBlank @Size(max = 100) String> usernames,
            @NotBlank @Size(max = 255) String titleAr,
            @NotBlank @Size(max = 255) String titleEn,
            @NotBlank @Size(max = 1000) String messageAr,
            @NotBlank @Size(max = 1000) String messageEn,
            @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+") String notificationType,
            @NotBlank @Pattern(regexp = "CRITICAL|HIGH|MEDIUM|INFO") String priority,
            @Size(max = 500) @Pattern(regexp = "^/.*") String actionLink) {
    }

    public record BulkSendResult(String bulkId, int requested, int created, int skippedMissing, int skippedInactive) {
    }
}
