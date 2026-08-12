package com.bemo.hr.notification.push;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hr.notifications.web-push")
@Getter
@Setter
public class WebPushProperties {
    private boolean enabled = false;
    private String publicKey = "";
    private String privateKey = "";
    private String subject = "";
    private int ttlSeconds = 86_400;

    public boolean configured() {
        if (!enabled) return false;
        if (publicKey == null || publicKey.isBlank()) return false;
        if (privateKey == null || privateKey.isBlank()) return false;
        if (subject == null || subject.isBlank()) return false;
        String normalized = subject.trim().toLowerCase();
        return normalized.startsWith("mailto:") || normalized.startsWith("https://");
    }

    public int safeTtlSeconds() {
        return Math.max(60, Math.min(ttlSeconds, 2_419_200));
    }
}
