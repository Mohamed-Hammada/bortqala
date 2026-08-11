package org.example.zkteco.core;

import java.util.List;
import java.util.Set;

public record DeviceProfile(
        String manufacturer,
        String family,
        String modelPattern,
        DeviceFamily deviceFamily,
        List<DeviceProtocol> preferredProtocols,
        Set<DeviceCapability> expectedCapabilities,
        String verificationStatus,
        String notes
) {
    public DeviceProfile {
        manufacturer = manufacturer == null ? "ZKTeco" : manufacturer;
        preferredProtocols = preferredProtocols == null ? List.of() : List.copyOf(preferredProtocols);
        expectedCapabilities = expectedCapabilities == null ? Set.of() : Set.copyOf(expectedCapabilities);
        verificationStatus = verificationStatus == null ? "seed" : verificationStatus;
        notes = notes == null ? "" : notes;
    }
}
