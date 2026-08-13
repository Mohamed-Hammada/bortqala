package org.example.zkteco.core;

import java.time.Instant;
import java.util.Map;

public record AttendanceEvent(
        String sourceEventId,
        String deviceSerial,
        String personExternalId,
        Instant occurredAt,
        String punchState,
        String verificationMethod,
        Map<String, String> attributes
) {
    public AttendanceEvent {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
