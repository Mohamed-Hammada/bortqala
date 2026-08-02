package com.bemo.hr.shared.security;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieCodec {

    public String encode(String appId, String rawToken) {
        if (appId == null || appId.isBlank() || rawToken == null || rawToken.isBlank()) {
            throw new BusinessRuleException("Session is invalid or expired.", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        return appId + ":" + rawToken;
    }

    public Decoded decode(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            throw new BusinessRuleException("Session is invalid or expired.", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        int separator = cookieValue.indexOf(':');
        if (separator <= 0 || separator == cookieValue.length() - 1) {
            throw new BusinessRuleException("Session is invalid or expired.", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED);
        }
        return new Decoded(cookieValue.substring(0, separator), cookieValue.substring(separator + 1));
    }

    public record Decoded(String appId, String rawToken) {
    }
}
