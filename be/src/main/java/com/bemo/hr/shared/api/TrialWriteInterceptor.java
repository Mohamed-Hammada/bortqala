package com.bemo.hr.shared.api;

import com.bemo.hr.product.trial.TrialDemoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@RequiredArgsConstructor
public class TrialWriteInterceptor implements HandlerInterceptor {
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");
    private final TrialDemoService service;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (SAFE.contains(request.getMethod()) || uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/platform/trial") || uri.startsWith("/api/v1/product/subscriptions") || uri.startsWith("/api/v1/platform/subscriptions"))
            return true;
        service.assertWriteAllowed();
        return true;
    }
}
