package org.example.zkteco.core;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record DeviceProbeResult(
        boolean online,
        DeviceProtocol protocol,
        String model,
        String serialNumber,
        String firmwareVersion,
        String platform,
        Set<DeviceCapability> capabilities,
        List<String> warnings,
        Instant checkedAt
) {
    public DeviceProbeResult {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }

    public static DeviceProbeResult offline(DeviceProtocol protocol, String warning) {
        return new DeviceProbeResult(false, protocol, null, null, null, null,
                Set.of(), List.of(warning), Instant.now());
    }
}
