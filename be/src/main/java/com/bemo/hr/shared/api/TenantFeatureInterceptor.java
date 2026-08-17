package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.EntitlementCatalog;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class TenantFeatureInterceptor implements HandlerInterceptor {
    private final TenantFeatureService featureService;
    private final EntitlementCatalog catalog;

    public TenantFeatureInterceptor(TenantFeatureService featureService, EntitlementCatalog catalog) {
        this.featureService = featureService;
        this.catalog = catalog;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String appId = TenantContext.currentOrSystem();

        if ("SYSTEM".equals(appId) || appId == null) {
            return true;
        }

        var required = catalog.requiredFeature(uri);
        if (required.isPresent() && !featureService.isEnabled(appId, required.get())) {
            throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
        }
        return true;
    }
}
