package org.example.zkteco.gateway.device;

import org.example.zkteco.core.AdapterRegistry;
import org.example.zkteco.core.DeviceEndpoint;
import org.example.zkteco.core.DeviceProbeResult;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceRegistryService {
    private final Map<UUID, DeviceEndpoint> devices = new ConcurrentHashMap<>();
    private final AdapterRegistry adapterRegistry;

    public DeviceRegistryService(AdapterRegistry adapterRegistry) {
        this.adapterRegistry = adapterRegistry;
    }

    public DeviceEndpoint save(DeviceEndpoint endpoint) {
        devices.put(endpoint.id(), endpoint);
        return endpoint;
    }

    public List<DeviceEndpoint> findAll() {
        return devices.values().stream()
                .sorted(Comparator.comparing(DeviceEndpoint::label))
                .toList();
    }

    public DeviceEndpoint require(UUID id) {
        DeviceEndpoint endpoint = devices.get(id);
        if (endpoint == null) {
            throw new DeviceNotFoundException(id);
        }
        return endpoint;
    }

    public DeviceProbeResult probe(UUID id) {
        DeviceEndpoint endpoint = require(id);
        return adapterRegistry.select(endpoint).probe(endpoint);
    }
}
