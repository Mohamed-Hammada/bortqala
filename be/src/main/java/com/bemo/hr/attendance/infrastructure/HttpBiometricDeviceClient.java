package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.application.BiometricDeviceClient;
import com.bemo.hr.attendance.domain.BiometricDevice;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class HttpBiometricDeviceClient implements BiometricDeviceClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();
    private final URI deviceHubBaseUri;
    private final String deviceHubApiKey;

    public HttpBiometricDeviceClient(
            ObjectMapper objectMapper,
            @Value("${BEMO_DEVICE_HUB_BASE_URL:http://localhost:8090}") String deviceHubBaseUrl,
            @Value("${DEVICE_HUB_API_KEY:}") String deviceHubApiKey) {
        this.objectMapper = objectMapper;
        this.deviceHubBaseUri = URI.create(deviceHubBaseUrl == null || deviceHubBaseUrl.isBlank()
                ? "http://localhost:8090"
                : deviceHubBaseUrl.strip());
        this.deviceHubApiKey = deviceHubApiKey == null ? "" : deviceHubApiKey.strip();
    }

    @Override
    public DeviceResponse fetch(BiometricDevice device, BiometricDeviceClient.DeviceCredentials credentials) {
        try {
            URI endpoint = endpoint(device);
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json");
            if (isDeviceHubEndpoint(endpoint) && !deviceHubApiKey.isBlank()) {
                builder.header("X-Device-Hub-Key", deviceHubApiKey);
            }
            if (credentials != null && credentials.password() != null && !credentials.password().isBlank()) {
                String user = credentials.username() == null ? "" : credentials.username();
                String basic = Base64.getEncoder().encodeToString(
                        (user + ":" + credentials.password()).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + basic);
            }
            HttpResponse<byte[]> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = "Failed to connect to biometric device. Response code: " + response.statusCode();
                if (response.statusCode() == 501) {
                    message = "The selected integration path does not yet include a complete attendance log reader for this version. Check the implementation status and official documentation.";
                }
                throw new BusinessRuleException(message, "BIO_DEVICE_HTTP_ERROR_" + response.statusCode(), HttpStatus.CONFLICT);
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rows = root.isArray() ? root : root.path("punches");
            if (!rows.isArray()) {
                throw new BusinessRuleException("Invalid device response: expected a punches array.", "BIO_DEVICE_RESPONSE_INVALID", HttpStatus.CONFLICT);
            }
            if (rows.size() > 10_000) {
                throw new BusinessRuleException("Device response exceeds the maximum of 10000 punches per sync.", "BIO_DEVICE_RESPONSE_TOO_LARGE", HttpStatus.CONFLICT);
            }
            List<DevicePunch> punches = new ArrayList<>();
            for (JsonNode row : rows) {
                String userId = firstText(row, "deviceUserId", "userId", "pin");
                String timestamp = firstText(row, "punchedAt", "timestamp", "dateTime");
                if (userId == null || timestamp == null) continue;
                Instant punchedAt = parseInstant(timestamp);
                punches.add(new DevicePunch(userId, firstText(row, "employeeName", "name"),
                        punchedAt, objectMapper.writeValueAsString(row)));
            }
            return new DeviceResponse(response.body(), List.copyOf(punches));
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("Failed to connect to biometric device or read its response: " + exception.getMessage(), "BIO_DEVICE_CONNECTION_FAILED", HttpStatus.CONFLICT);
        }
    }

    private URI endpoint(BiometricDevice device) {
        URI base = URI.create(device.getEndpointUrl());
        if (!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme())) {
            throw new BusinessRuleException("Biometric device URL must start with http or https.", "BIO_DEVICE_ENDPOINT_SCHEME_REQUIRED", HttpStatus.CONFLICT);
        }
        if (base.getHost() == null)
            throw new BusinessRuleException("Biometric device URL is malformed.", "BIO_DEVICE_ENDPOINT_MALFORMED", HttpStatus.CONFLICT);
        if (device.getLastSuccessfulPunchAt() == null) return base;
        String separator = base.getQuery() == null ? "?" : "&";
        return URI.create(base + separator + "since=" + URLEncoder.encode(
                device.getLastSuccessfulPunchAt().toString(), StandardCharsets.UTF_8));
    }

    private boolean isDeviceHubEndpoint(URI endpoint) {
        if (endpoint.getHost() == null || deviceHubBaseUri.getHost() == null) return false;
        if (!endpoint.getHost().equalsIgnoreCase(deviceHubBaseUri.getHost())) return false;
        return effectivePort(endpoint) == effectivePort(deviceHubBaseUri)
                && endpoint.getPath() != null
                && endpoint.getPath().startsWith("/v1/devices/");
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String firstText(JsonNode row, String... fields) {
        for (String field : fields) {
            JsonNode value = row.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) return value.asText().strip();
        }
        return null;
    }

    private Instant parseInstant(String value) {
        try {
            if (value.chars().allMatch(Character::isDigit)) return Instant.ofEpochMilli(Long.parseLong(value));
            return Instant.parse(value);
        } catch (Exception exception) {
            throw new BusinessRuleException("Invalid punch timestamp in device response: " + value, "BIO_DEVICE_INVALID_TIMESTAMP", HttpStatus.CONFLICT);
        }
    }
}
