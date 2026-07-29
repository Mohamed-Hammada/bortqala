package com.bemo.hr.attendance.infrastructure;

import com.bemo.hr.attendance.application.BiometricDeviceClient;
import com.bemo.hr.attendance.domain.BiometricDevice;
import com.bemo.hr.shared.domain.BusinessRuleException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class HttpBiometricDeviceClient implements BiometricDeviceClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    public HttpBiometricDeviceClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceResponse fetch(BiometricDevice device) {
        try {
            URI endpoint = endpoint(device);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessRuleException("فشل الاتصال بجهاز البصمة. رمز الاستجابة: " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rows = root.isArray() ? root : root.path("punches");
            if (!rows.isArray()) {
                throw new BusinessRuleException("استجابة الجهاز غير صالحة: يجب إرسال مصفوفة punches.");
            }
            if (rows.size() > 10_000) {
                throw new BusinessRuleException("استجابة الجهاز تتجاوز الحد الأقصى وهو 10000 بصمة لكل مزامنة.");
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
            throw new BusinessRuleException("تعذر الاتصال بجهاز البصمة أو قراءة استجابته: " + exception.getMessage());
        }
    }

    private URI endpoint(BiometricDevice device) {
        URI base = URI.create(device.getEndpointUrl());
        if (!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme())) {
            throw new BusinessRuleException("رابط جهاز البصمة يجب أن يبدأ بـ http أو https.");
        }
        if (base.getHost() == null) throw new BusinessRuleException("رابط جهاز البصمة غير صالح.");
        if (device.getLastSuccessfulPunchAt() == null) return base;
        String separator = base.getQuery() == null ? "?" : "&";
        return URI.create(base + separator + "since=" + URLEncoder.encode(
                device.getLastSuccessfulPunchAt().toString(), StandardCharsets.UTF_8));
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
            throw new BusinessRuleException("وقت بصمة غير صالح في استجابة الجهاز: " + value);
        }
    }
}
