package org.example.zkteco.core;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DeviceEndpoint(
        UUID id,
        String label,
        DeviceProtocol preferredProtocol,
        String host,
        Integer port,
        URI baseUri,
        String secretRef,
        Map<String, String> properties
) {
    public DeviceEndpoint {
        id = id == null ? UUID.randomUUID() : id;
        label = requireText(label, "label");
        preferredProtocol = preferredProtocol == null ? DeviceProtocol.AUTO : preferredProtocol;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        if (port != null && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public int portOr(int fallback) {
        return port == null ? fallback : port;
    }

    public String property(String key, String fallback) {
        return properties.getOrDefault(key, fallback);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
