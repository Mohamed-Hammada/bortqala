package com.bemo.hr.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("hr.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl, Duration refreshTtl) {
}
