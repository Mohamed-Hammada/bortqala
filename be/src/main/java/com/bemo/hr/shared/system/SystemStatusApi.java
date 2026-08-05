package com.bemo.hr.shared.system;

import jakarta.validation.constraints.Size;

public final class SystemStatusApi {
    private SystemStatusApi() {
    }

    public record StatusResponse(
            String status,
            String service,
            String version,
            String cacheVersion,
            long serverTime,
            Long cacheUpdatedAt,
            String cacheUpdatedBy,
            boolean demoNoLoginEnabled) {
    }

    public record RotateCacheRequest(@Size(max = 500) String reason) {
    }
}
