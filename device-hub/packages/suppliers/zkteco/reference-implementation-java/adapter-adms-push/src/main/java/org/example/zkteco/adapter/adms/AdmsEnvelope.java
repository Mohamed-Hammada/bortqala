package org.example.zkteco.adapter.adms;

import java.time.Instant;
import java.util.Map;

public record AdmsEnvelope(
        long sequence,
        String serialNumber,
        String path,
        String method,
        Map<String, String> query,
        String contentType,
        String body,
        Instant receivedAt
) {
    public AdmsEnvelope {
        query = query == null ? Map.of() : Map.copyOf(query);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
