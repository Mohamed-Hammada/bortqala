package com.bemo.hr.attendance.application;

import com.bemo.hr.attendance.api.DeviceIntegrationApi;
import com.bemo.hr.attendance.api.ImportApi;
import com.bemo.hr.attendance.domain.BiometricDevice;
import com.bemo.hr.attendance.domain.BiometricDeviceIntegration;
import com.bemo.hr.attendance.infrastructure.BiometricDeviceIntegrationRepository;
import com.bemo.hr.attendance.infrastructure.BiometricDeviceRepository;
import com.bemo.hr.attendance.infrastructure.DeviceCredentialsCrypto;
import com.bemo.hr.attendance.infrastructure.VendorHubClient;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceIntegrationService {
    private final BiometricDeviceIntegrationRepository integrationRepository;
    private final BiometricDeviceRepository biometricDeviceRepository;
    private final BiometricDeviceSyncService biometricDeviceSyncService;
    private final DeviceCredentialsCrypto credentialsCrypto;
    private final VendorHubClient vendorHubClient;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public JsonNode health() {
        return vendorHubClient.health();
    }

    public JsonNode suppliers() {
        return vendorHubClient.suppliers();
    }

    public JsonNode routes(String vendor) {
        return vendorHubClient.routes(vendor);
    }

    public DeviceIntegrationApi.RouteResolution resolve(DeviceIntegrationApi.RouteRequest request) {
        return resolution(vendorHubClient.resolve(request));
    }

    public List<DeviceIntegrationApi.DeviceResponse> list() {
        return integrationRepository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    public DeviceIntegrationApi.DeviceResponse get(String id) {
        return response(require(id));
    }

    @Transactional
    public DeviceIntegrationApi.DeviceResponse create(DeviceIntegrationApi.DeviceRequest request, String actor) {
        DeviceIntegrationApi.RouteResolution resolution = resolution(vendorHubClient.resolve(request));
        String route = selectRoute(request.route(), resolution);
        DeviceIntegrationApi.RouteCandidate candidate = candidate(route, resolution);
        assertCompatible(candidate);

        JsonNode registered = vendorHubClient.register(request, route);
        String hubDeviceId = registered.path("id").asText();
        if (hubDeviceId.isBlank()) {
            throw new BusinessRuleException("Device hub did not return a device id.", "DEVICE_HUB_REGISTRATION_INVALID", HttpStatus.CONFLICT);
        }

        try {
            ImportApi.DeviceResponse biometricDevice = biometricDeviceSyncService.create(
                    new ImportApi.DeviceRequest(
                            request.name(),
                            vendorHubClient.punchesUrl(hubDeviceId),
                            request.enabled(),
                            normalizedInterval(request.syncIntervalMinutes()),
                            request.username(),
                            request.password()),
                    actor);

            BiometricDeviceIntegration integration = new BiometricDeviceIntegration(biometricDevice.id(), hubDeviceId);
            apply(integration, request, route, candidate, resolution);
            integrationRepository.saveAndFlush(integration);
            auditService.record("CREATE", "BIOMETRIC_DEVICE_INTEGRATION", integration.getId(), actor,
                    auditPayload(integration), null);
            return response(integration);
        } catch (RuntimeException exception) {
            try {
                vendorHubClient.delete(hubDeviceId);
            } catch (RuntimeException ignored) {
                // Best-effort compensation. The hub registry can also be cleaned from the UI/CLI.
            }
            throw exception;
        }
    }

    @Transactional
    public DeviceIntegrationApi.DeviceResponse update(String id, DeviceIntegrationApi.DeviceRequest request, String actor) {
        BiometricDeviceIntegration integration = require(id);
        DeviceIntegrationApi.RouteResolution resolution = resolution(vendorHubClient.resolve(request));
        String route = selectRoute(request.route(), resolution);
        DeviceIntegrationApi.RouteCandidate candidate = candidate(route, resolution);
        assertCompatible(candidate);

        vendorHubClient.update(integration.getHubDeviceId(), request, route);
        biometricDeviceSyncService.update(
                integration.getBiometricDeviceId(),
                new ImportApi.DeviceRequest(
                        request.name(),
                        vendorHubClient.punchesUrl(integration.getHubDeviceId()),
                        request.enabled(),
                        normalizedInterval(request.syncIntervalMinutes()),
                        request.username(),
                        request.password()),
                actor);
        apply(integration, request, route, candidate, resolution);
        integrationRepository.saveAndFlush(integration);
        auditService.record("UPDATE", "BIOMETRIC_DEVICE_INTEGRATION", integration.getId(), actor,
                auditPayload(integration), null);
        return response(integration);
    }

    @Transactional
    public DeviceIntegrationApi.ProbeResponse probe(String id, String actor) {
        BiometricDeviceIntegration integration = require(id);
        BiometricDevice biometricDevice = biometricDeviceRepository.findById(integration.getBiometricDeviceId())
                .orElseThrow(() -> new NotFoundException("Linked biometric device was not found.", "BIO_DEVICE_NOT_FOUND"));
        String password = credentialsCrypto.decrypt(biometricDevice.getPasswordEncrypted());
        JsonNode result = vendorHubClient.probe(integration.getHubDeviceId(), biometricDevice.getUsername(), password);
        boolean ok = result.path("ok").asBoolean(false);
        String detail = result.path("detail").asText(ok ? "Probe succeeded." : "Probe failed.");
        integration.probeResult(ok, detail);
        integrationRepository.saveAndFlush(integration);
        auditService.record("PROBE", "BIOMETRIC_DEVICE_INTEGRATION", integration.getId(), actor,
                "{\"ok\":" + ok + ",\"route\":\"" + safe(integration.getRoute()) + "\"}", null);
        return new DeviceIntegrationApi.ProbeResponse(
                ok ? "SUCCESS" : "FAILED",
                ok,
                result.path("route").asText(integration.getRoute()),
                detail,
                jsonValue(result.path("data")),
                Instant.now());
    }

    @Transactional
    public ImportApi.DeviceSyncResponse sync(String id, String actor) {
        BiometricDeviceIntegration integration = require(id);
        return biometricDeviceSyncService.sync(integration.getBiometricDeviceId(), actor);
    }

    private void apply(
            BiometricDeviceIntegration integration,
            DeviceIntegrationApi.DeviceRequest request,
            String route,
            DeviceIntegrationApi.RouteCandidate candidate,
            DeviceIntegrationApi.RouteResolution resolution) {
        integration.updateConfiguration(
                request.name(),
                request.vendor(),
                request.model(),
                request.serialNumber(),
                request.firmwareVersion(),
                request.platformVersion(),
                request.serverVersion(),
                request.osName(),
                request.architecture(),
                json(request.sdkVersions() == null ? Map.of() : request.sdkVersions()),
                json(request.apiVersions() == null ? Map.of() : request.apiVersions()),
                json(request.capabilityHints() == null ? List.of() : request.capabilityHints()),
                request.host(),
                request.port(),
                request.baseUrl(),
                route,
                candidate.status(),
                candidate.kind(),
                candidate.implementationStatus(),
                json(resolution.officialDocumentation()),
                json(request.options() == null ? Map.of() : request.options()));
    }

    private DeviceIntegrationApi.DeviceResponse response(BiometricDeviceIntegration integration) {
        BiometricDevice device = biometricDeviceRepository.findById(integration.getBiometricDeviceId()).orElse(null);
        return new DeviceIntegrationApi.DeviceResponse(
                integration.getId(),
                integration.getBiometricDeviceId(),
                integration.getHubDeviceId(),
                integration.getName(),
                integration.getVendor(),
                integration.getModel(),
                integration.getSerialNumber(),
                integration.getFirmwareVersion(),
                integration.getPlatformVersion(),
                integration.getServerVersion(),
                integration.getOsName(),
                integration.getArchitecture(),
                stringMap(integration.getSdkVersionsJson()),
                stringMap(integration.getApiVersionsJson()),
                stringList(integration.getCapabilityHintsJson()),
                integration.getHost(),
                integration.getPort(),
                integration.getBaseUrl(),
                integration.getRoute(),
                integration.getRouteStatus(),
                integration.getRouteKind(),
                integration.getImplementationStatus(),
                stringList(integration.getOfficialDocumentationJson()),
                objectMap(integration.getOptionsJson()),
                device != null && device.isEnabled(),
                device == null ? 15 : device.getSyncIntervalMinutes(),
                device == null ? null : device.getUsername(),
                device != null && device.hasPassword(),
                integration.getLastProbeStatus(),
                integration.getLastProbeMessage(),
                integration.getLastProbeAt(),
                integration.getCreatedAt(),
                integration.getUpdatedAt());
    }

    private DeviceIntegrationApi.RouteResolution resolution(JsonNode root) {
        List<DeviceIntegrationApi.RouteCandidate> candidates = new ArrayList<>();
        JsonNode candidateNodes = root.path("candidates");
        if (candidateNodes.isArray()) {
            for (JsonNode item : candidateNodes) {
                candidates.add(new DeviceIntegrationApi.RouteCandidate(
                        item.path("route").asText(),
                        item.path("kind").asText("other"),
                        item.path("status").asText("UNKNOWN"),
                        item.path("reason").asText(),
                        item.path("sdk_version_spec").asText("*"),
                        item.path("api_version_spec").asText("*"),
                        item.path("server_version_spec").asText("*"),
                        item.path("implementation_status").asText("scaffold"),
                        strings(item.path("official_documentation"))));
            }
        }
        return new DeviceIntegrationApi.RouteResolution(
                root.path("supplier").asText(),
                root.path("model_pattern").asText(),
                root.path("generation_or_version").asText(),
                nullIfBlank(root.path("preferred_route").asText()),
                strings(root.path("compatible_routes")),
                List.copyOf(candidates),
                root.path("notes").asText(),
                strings(root.path("official_documentation")));
    }

    private String selectRoute(String requested, DeviceIntegrationApi.RouteResolution resolution) {
        String route = requested == null || requested.isBlank() ? resolution.preferredRoute() : requested.strip();
        if (route == null || route.isBlank()) {
            String states = resolution.candidates().stream()
                    .map(candidate -> candidate.route() + ":" + candidate.status())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("no candidates");
            throw new BusinessRuleException(
                    "No version-verified integration route can be selected yet. " + states,
                    "DEVICE_INTEGRATION_VERSION_REQUIRED",
                    HttpStatus.CONFLICT);
        }
        return route;
    }

    private DeviceIntegrationApi.RouteCandidate candidate(String route, DeviceIntegrationApi.RouteResolution resolution) {
        return resolution.candidates().stream()
                .filter(item -> item.route().equals(route))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "The selected integration route is not available for this supplier/model profile.",
                        "DEVICE_INTEGRATION_ROUTE_UNAVAILABLE",
                        HttpStatus.CONFLICT));
    }

    private void assertCompatible(DeviceIntegrationApi.RouteCandidate candidate) {
        if (!"COMPATIBLE".equals(candidate.status())) {
            throw new BusinessRuleException(
                    "Integration route " + candidate.route() + " is not version-compatible yet: "
                            + candidate.status() + " - " + candidate.reason(),
                    "DEVICE_INTEGRATION_ROUTE_NOT_COMPATIBLE",
                    HttpStatus.CONFLICT);
        }
    }

    private BiometricDeviceIntegration require(String id) {
        return integrationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Device integration was not found.", "DEVICE_INTEGRATION_NOT_FOUND"));
    }

    private int normalizedInterval(int value) {
        return value <= 0 ? 15 : Math.min(1440, value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new BusinessRuleException("Unable to serialize integration configuration.", "DEVICE_INTEGRATION_CONFIG_INVALID", HttpStatus.CONFLICT);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> stringMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            Map<?, ?> raw = objectMapper.readValue(value, Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            raw.forEach((key, item) -> result.put(String.valueOf(key), item == null ? "" : String.valueOf(item)));
            return Collections.unmodifiableMap(result);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(String value) {
        if (value == null || value.isBlank()) return Map.of();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>((Map<String, Object>) objectMapper.readValue(value, Map.class)));
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<String> stringList(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            List<?> raw = objectMapper.readValue(value, List.class);
            return raw.stream().map(String::valueOf).toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> strings(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.asText().isBlank()) values.add(item.asText());
        }
        return List.copyOf(values);
    }

    private Object jsonValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return Map.of();
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(node), Object.class);
        } catch (Exception ignored) {
            return node.asText();
        }
    }

    private String auditPayload(BiometricDeviceIntegration integration) {
        return "{\"vendor\":\"" + safe(integration.getVendor())
                + "\",\"model\":\"" + safe(integration.getModel())
                + "\",\"route\":\"" + safe(integration.getRoute()) + "\"}";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
