package com.bemo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Performance logging settings.
 *
 * <pre>
 * shared.performance-logging.default.enabled=true
 * shared.performance-logging.default.min-duration-in-millis=50
 * </pre>
 */
@ConfigurationProperties(prefix = "shared.performance-logging")
public record PerformanceLoggingProperties(DefaultConfig defaults) {

    public PerformanceLoggingProperties {
        if (defaults == null) {
            defaults = new DefaultConfig(true, 50);
        }
    }

    public record DefaultConfig(@DefaultValue("true") boolean enabled,
                                @DefaultValue("50") long minDurationInMillis) {

        public DefaultConfig {
            if (minDurationInMillis < 0) {
                throw new IllegalArgumentException("min-duration-in-millis must be >= 0");
            }
        }
    }

    public boolean enabled() {
        return defaults != null && defaults.enabled();
    }

    public long minDurationInMillis() {
        return defaults == null ? 50 : defaults.minDurationInMillis();
    }
}
