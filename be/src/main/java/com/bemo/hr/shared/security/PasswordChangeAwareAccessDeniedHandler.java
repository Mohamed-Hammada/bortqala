package com.bemo.hr.shared.security;

import com.bemo.hr.shared.api.ApiError;
import com.bemo.hr.shared.observability.RequestAuditFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

public class PasswordChangeAwareAccessDeniedHandler implements AccessDeniedHandler {

    public static final String PASSWORD_CHANGE_AUTHORITY = "ROLE_PASSWORD_CHANGE";

    private final ObjectMapper objectMapper;

    public PasswordChangeAwareAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean passwordChangeRequired = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(PASSWORD_CHANGE_AUTHORITY));
        String code = passwordChangeRequired ? "PASSWORD_CHANGE_REQUIRED" : "FORBIDDEN";
        String message = passwordChangeRequired
                ? "You must change your password before continuing."
                : "Access denied.";
        Object correlationId = request.getAttribute(RequestAuditFilter.REQUEST_ATTRIBUTE_CORRELATION_ID);
        ApiError error = new ApiError(code, message, message, HttpServletResponse.SC_FORBIDDEN,
                request.getRequestURI(), correlationId != null ? correlationId.toString() : null, Instant.now(), null);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
