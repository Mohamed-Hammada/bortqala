package com.bemo.hr.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("hr.security.demo-no-login")
public record DemoNoLoginProperties(
        boolean enabled,
        String secret,
        String appCode,
        String appName,
        String profiles) {
}
