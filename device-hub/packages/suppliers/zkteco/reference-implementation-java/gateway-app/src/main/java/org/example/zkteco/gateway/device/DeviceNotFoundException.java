package org.example.zkteco.gateway.device;

import java.util.UUID;

public final class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(UUID id) {
        super("Device not found: " + id);
    }
}
