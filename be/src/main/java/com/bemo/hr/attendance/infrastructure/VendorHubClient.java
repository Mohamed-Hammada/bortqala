package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.api.DeviceIntegrationApi;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VendorHubClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public VendorHubClient(
            ObjectMapper objectMapper,
            @Value("${BEMO_DEVICE_HUB_BASE_URL:http://localhost:8090}") String baseUrl,
            @Value("${DEVICE_HUB_API_KEY:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl == null ? "http://localhost:8090" : baseUrl.strip().replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    }

    public JsonNode health() {
        return request("GET", "/health", null, null, null);
    }

    public JsonNode suppliers() {
        return request("GET", "/v1/suppliers", null, null, null);
    }

    public JsonNode routes(String vendor) {
        return request("GET", "/v1/suppliers/" + pathSegment(vendor) + "/routes", null, null, null);
    }

    public JsonNode resolve(DeviceIntegrationApi.RouteRequest input) {
        return request("POST", "/v1/resolve-route", routePayload(input), null, null);
    }

    public JsonNode resolve(DeviceIntegrationApi.DeviceRequest input) {
        return request("POST", "/v1/resolve-route", devicePayload(input, input.route(), false), null, null);
    }

    public JsonNode register(DeviceIntegrationApi.DeviceRequest input, String route) {
        return request("POST", "/v1/devices", devicePayload(input, route, false), null, null);
    }

    public JsonNode update(String hubDeviceId, DeviceIntegrationApi.DeviceRequest input, String route) {
        return request("PUT", "/v1/devices/" + pathSegment(hubDeviceId), devicePayload(input, route, false), null, null);
    }

    public void delete(String hubDeviceId) {
        request("DELETE", "/v1/devices/" + pathSegment(hubDeviceId), null, null, null);
    }

    public JsonNode probe(String hubDeviceId, String username, String password) {
        return request("POST", "/v1/devices/" + pathSegment(hubDeviceId) + "/probe", Map.of(), username, password);
    }

    public String punchesUrl(String hubDeviceId) {
        return baseUrl + "/v1/devices/" + pathSegment(hubDeviceId) + "/punches";
    }

    public String baseUrl() {
        return baseUrl;
    }

    private Map<String, Object> routePayload(DeviceIntegrationApi.RouteRequest input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "route-check");
        payload.put("vendor", normalized(input.vendor()));
        payload.put("model", defaultText(input.model(), "UNKNOWN"));
        payload.put("firmware", defaultText(input.firmwareVersion(), ""));
        payload.put("platform_version", defaultText(input.platformVersion(), ""));
        payload.put("server_version", defaultText(input.serverVersion(), ""));
        payload.put("os_name", defaultText(input.osName(), ""));
        payload.put("architecture", defaultText(input.architecture(), ""));
        payload.put("sdk_versions", safeMap(input.sdkVersions()));
        payload.put("api_versions", safeMap(input.apiVersions()));
        payload.put("capability_hints", safeList(input.capabilityHints()));
        payload.put("host", emptyToNull(input.host()));
        payload.put("port", input.port());
        payload.put("base_url", emptyToNull(input.baseUrl()));
        payload.put("route", emptyToNull(input.route()));
        payload.put("options", safeObjectMap(input.options()));
        return payload;
    }

    private Map<String, Object> devicePayload(DeviceIntegrationApi.DeviceRequest input, String route, boolean includePassword) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", input.name());
        payload.put("vendor", normalized(input.vendor()));
        payload.put("model", defaultText(input.model(), "UNKNOWN"));
        payload.put("firmware", defaultText(input.firmwareVersion(), ""));
        payload.put("platform_version", defaultText(input.platformVersion(), ""));
        payload.put("server_version", defaultText(input.serverVersion(), ""));
        payload.put("os_name", defaultText(input.osName(), ""));
        payload.put("architecture", defaultText(input.architecture(), ""));
        payload.put("sdk_versions", safeMap(input.sdkVersions()));
        payload.put("api_versions", safeMap(input.apiVersions()));
        payload.put("capability_hints", safeList(input.capabilityHints()));
        payload.put("host", emptyToNull(input.host()));
        payload.put("port", input.port());
        payload.put("base_url", emptyToNull(input.baseUrl()));
        payload.put("route", emptyToNull(route));
        payload.put("username", emptyToNull(input.username()));
        // Credentials are encrypted and retained by Bortqala. The hub receives them only per probe/sync request.
        payload.put("password", includePassword ? emptyToNull(input.password()) : null);
        payload.put("options", safeObjectMap(input.options()));
        return payload;
    }

    private JsonNode request(String method, String path, Object body, String username, String password) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(25))
                    .header("Accept", "application/json");
            if (!apiKey.isBlank()) builder.header("X-Device-Hub-Key", apiKey);
            if (password != null && !password.isBlank()) {
                String basic = Base64.getEncoder().encodeToString(
                        ((username == null ? "" : username) + ":" + password).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + basic);
            }
            if (body != null) {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode payload = response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String detail = payload.path("detail").asText();
                if (detail.isBlank()) detail = "Device hub returned HTTP " + response.statusCode();
                throw new BusinessRuleException(detail, "DEVICE_HUB_REQUEST_FAILED", HttpStatus.CONFLICT);
            }
            return payload;
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException(
                    "Device integration hub is unavailable: " + exception.getMessage(),
                    "DEVICE_HUB_UNAVAILABLE",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private String pathSegment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._-]+")) {
            throw new BusinessRuleException("Invalid device-hub identifier.", "DEVICE_HUB_ID_INVALID", HttpStatus.CONFLICT);
        }
        return value;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.strip().toLowerCase();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static Map<String, String> safeMap(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    private static Map<String, Object> safeObjectMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private static List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }
}
