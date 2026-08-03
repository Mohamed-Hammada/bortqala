package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class TenantFeatureInterceptor implements HandlerInterceptor {

    private final TenantFeatureService featureService;

    public TenantFeatureInterceptor(TenantFeatureService featureService) {
        this.featureService = featureService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String appId = TenantContext.currentOrSystem();
        
        if ("SYSTEM".equals(appId) || appId == null) {
            return true;
        }

        if (uri.startsWith("/api/v1/payroll") && !featureService.isEnabled(appId, "payroll.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        if (uri.startsWith("/api/v1/trade/sales") && !featureService.isEnabled(appId, "sales.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        if (uri.startsWith("/api/v1/manufacturing") && !featureService.isEnabled(appId, "manufacturing.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        if (uri.startsWith("/api/v1/finance") && !featureService.isEnabled(appId, "finance.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        if (uri.startsWith("/api/v1/fiscal-periods") && !featureService.isEnabled(appId, "finance.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        if (uri.startsWith("/api/v1/workforce/contractors") && !featureService.isEnabled(appId, "workforce.contractorAccounts.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        if (uri.startsWith("/api/v1/workforce/settlements") && !featureService.isEnabled(appId, "workforce.contractorAccounts.enabled")) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }

        return true;
    }
}
