package org.example.zkteco.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdapterRegistryTest {

    @Test
    void selectsExplicitProtocol() {
        DeviceAdapter pull = stub(DeviceProtocol.ZK_PULL);
        DeviceAdapter adms = stub(DeviceProtocol.ADMS_PUSH);
        AdapterRegistry registry = new AdapterRegistry(List.of(pull, adms));

        DeviceEndpoint endpoint = new DeviceEndpoint(UUID.randomUUID(), "clock",
                DeviceProtocol.ZK_PULL, "127.0.0.1", 4370, null, null, null);

        assertEquals(DeviceProtocol.ZK_PULL, registry.select(endpoint).protocol());
    }

    @Test
    void rejectsMissingProtocol() {
        AdapterRegistry registry = new AdapterRegistry(List.of(stub(DeviceProtocol.ADMS_PUSH)));
        DeviceEndpoint endpoint = new DeviceEndpoint(UUID.randomUUID(), "clock",
                DeviceProtocol.ZK_PULL, "127.0.0.1", 4370, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> registry.select(endpoint));
    }

    private static DeviceAdapter stub(DeviceProtocol protocol) {
        return new DeviceAdapter() {
            @Override public DeviceProtocol protocol() { return protocol; }
            @Override public boolean supports(DeviceEndpoint endpoint) { return true; }
            @Override public DeviceProbeResult probe(DeviceEndpoint endpoint) {
                return DeviceProbeResult.offline(protocol, "stub");
            }
        };
    }
}
