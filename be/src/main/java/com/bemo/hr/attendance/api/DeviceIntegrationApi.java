package com.bemo.hr.attendance.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DeviceIntegrationApi {
    private DeviceIntegrationApi() {
    }

    public record RouteRequest(
            @NotBlank String vendor,
            @NotBlank String model,
            String firmwareVersion,
            String platformVersion,
            String serverVersion,
            String osName,
            String architecture,
            Map<String, String> sdkVersions,
            Map<String, String> apiVersions,
            List<String> capabilityHints,
            String host,
            @Min(1) @Max(65535) Integer port,
            String baseUrl,
            String route,
            Map<String, Object> options
    ) {
    }

    public record DeviceRequest(
            @NotBlank String name,
            @NotBlank String vendor,
            @NotBlank String model,
            String serialNumber,
            String firmwareVersion,
            String platformVersion,
            String serverVersion,
            String osName,
            String architecture,
            Map<String, String> sdkVersions,
            Map<String, String> apiVersions,
            List<String> capabilityHints,
            String host,
            @Min(1) @Max(65535) Integer port,
            String baseUrl,
            String route,
            Map<String, Object> options,
            String username,
            String password,
            boolean enabled,
            @Min(1) @Max(1440) int syncIntervalMinutes
    ) {
    }

    public record RouteCandidate(
            String route,
            String kind,
            String status,
            String reason,
            String sdkVersionSpec,
            String apiVersionSpec,
            String serverVersionSpec,
            String implementationStatus,
            List<String> officialDocumentation
    ) {
    }

    public record RouteResolution(
            String supplier,
            String modelPattern,
            String generationOrVersion,
            String preferredRoute,
            List<String> compatibleRoutes,
            List<RouteCandidate> candidates,
            String notes,
            List<String> officialDocumentation
    ) {
    }

    public record DeviceResponse(
            String id,
            String biometricDeviceId,
            String hubDeviceId,
            String name,
            String vendor,
            String model,
            String serialNumber,
            String firmwareVersion,
            String platformVersion,
            String serverVersion,
            String osName,
            String architecture,
            Map<String, String> sdkVersions,
            Map<String, String> apiVersions,
            List<String> capabilityHints,
            String host,
            Integer port,
            String baseUrl,
            String route,
            String routeStatus,
            String routeKind,
            String implementationStatus,
            List<String> officialDocumentation,
            Map<String, Object> options,
            boolean enabled,
            int syncIntervalMinutes,
            String username,
            boolean hasPassword,
            String lastProbeStatus,
            String lastProbeMessage,
            Instant lastProbeAt,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ProbeResponse(
            String status,
            boolean ok,
            String route,
            String detail,
            Object data,
            Instant checkedAt
    ) {
    }
}
