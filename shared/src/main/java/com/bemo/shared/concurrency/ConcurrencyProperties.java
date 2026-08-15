package com.bemo.shared.concurrency;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Thread-pool sizing for the framework's default executor.
 *
 * <pre>
 * shared.concurrency.core-pool-size=8
 * shared.concurrency.max-pool-size=32
 * shared.concurrency.queue-capacity=2000
 * shared.concurrency.thread-name-prefix=worker
 * shared.concurrency.keep-alive-seconds=60
 * </pre>
 */
@ConfigurationProperties(prefix = "shared.concurrency")
public record ConcurrencyProperties(@DefaultValue("8") int corePoolSize,
                                    @DefaultValue("32") int maxPoolSize,
                                    @DefaultValue("2000") int queueCapacity,
                                    @DefaultValue("worker") String threadNamePrefix,
                                    @DefaultValue("60") int keepAliveSeconds) {

    public ConcurrencyProperties {
        if (corePoolSize < 1) {
            throw new IllegalArgumentException("core-pool-size must be >= 1");
        }
        if (maxPoolSize < corePoolSize) {
            throw new IllegalArgumentException("max-pool-size must be >= core-pool-size");
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queue-capacity must be >= 0");
        }
    }
}
