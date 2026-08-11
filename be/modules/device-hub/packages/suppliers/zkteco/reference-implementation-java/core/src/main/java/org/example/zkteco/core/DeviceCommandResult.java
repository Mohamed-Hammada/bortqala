package org.example.zkteco.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DeviceCommandResult(UUID commandId, boolean success, String message,
                                  Map<String,String> data, Instant completedAt) {
    public DeviceCommandResult {
        data = data == null ? Map.of() : Map.copyOf(data);
        completedAt = completedAt == null ? Instant.now() : completedAt;
    }
}
