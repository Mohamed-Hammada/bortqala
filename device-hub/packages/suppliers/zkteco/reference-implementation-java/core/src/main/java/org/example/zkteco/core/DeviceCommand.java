package org.example.zkteco.core;

import java.util.Map;
import java.util.UUID;

public record DeviceCommand(UUID id, String operation, Map<String,String> arguments) {
    public DeviceCommand {
        id = id == null ? UUID.randomUUID() : id;
        if (operation == null || operation.isBlank()) throw new IllegalArgumentException("operation is required");
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
