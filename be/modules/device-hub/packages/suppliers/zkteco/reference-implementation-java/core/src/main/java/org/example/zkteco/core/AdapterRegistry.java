package org.example.zkteco.core;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AdapterRegistry {
    private final List<DeviceAdapter> adapters;

    public AdapterRegistry(List<DeviceAdapter> adapters) {
        Objects.requireNonNull(adapters, "adapters");
        this.adapters = adapters.stream()
                .sorted(Comparator.comparing(adapter -> adapter.protocol().name()))
                .toList();
    }

    public List<DeviceAdapter> all() {
        return adapters;
    }

    public DeviceAdapter select(DeviceEndpoint endpoint) {
        if (endpoint.preferredProtocol() != DeviceProtocol.AUTO) {
            return adapters.stream()
                    .filter(adapter -> adapter.protocol() == endpoint.preferredProtocol())
                    .filter(adapter -> adapter.supports(endpoint))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No adapter supports protocol " + endpoint.preferredProtocol()));
        }

        return adapters.stream()
                .filter(adapter -> adapter.supports(endpoint))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No adapter supports this endpoint"));
    }
}
