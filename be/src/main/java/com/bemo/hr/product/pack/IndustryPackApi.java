package com.bemo.hr.product.pack;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class IndustryPackApi {
    private IndustryPackApi() {
    }

    public record InstallRequest(@NotBlank String operationId) {
    }

    public record ReconcileRequest(String operationId, String reason) {
    }

    public record UpgradeRequest(@NotBlank String operationId, long expectedVersion) {
    }

    public record SettingsRequest(@NotBlank @Size(max = 4000) String settingsJson, long expectedVersion) {
    }

    public record StepRequest(boolean skip, long expectedVersion) {
    }

    public record RoleReadinessResponse(String code, boolean required, boolean available, long assignedUsers, String status) {
    }

    public record TemplateDescriptorResponse(String key, String fileName, String workflow, boolean downloadable, String route) {
    }

    public record StepResponse(String id, String key, int sequence, String prerequisiteKey, boolean optional,
                               String status, long version) {
    }

    public record PackResponse(String code, String nameKey, String descriptionKey, int availableVersion,
                               Integer installedVersion, boolean upgradeAvailable, String status,
                               List<String> requiredFeatures, List<String> defaultRoles, List<String> kpis,
                               List<String> importTemplates, String settingsJson, boolean customized,
                               boolean goLiveReady, long version, List<StepResponse> steps,
                               List<RoleReadinessResponse> roleReadiness,
                               List<TemplateDescriptorResponse> templateBindings) {
        public PackResponse(String code, String nameKey, String descriptionKey, int availableVersion,
                            Integer installedVersion, boolean upgradeAvailable, String status,
                            List<String> requiredFeatures, List<String> defaultRoles, List<String> kpis,
                            List<String> importTemplates, String settingsJson, boolean customized,
                            boolean goLiveReady, long version, List<StepResponse> steps) {
            this(code, nameKey, descriptionKey, availableVersion, installedVersion, upgradeAvailable, status,
                    requiredFeatures, defaultRoles, kpis, importTemplates, settingsJson, customized,
                    goLiveReady, version, steps, List.of(), List.of());
        }
    }
}
