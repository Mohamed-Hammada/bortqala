package com.bemo.hr.shared.api;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

public class TenantFeatureInterceptor implements HandlerInterceptor {

    private static final Map<String, String> GATED_PREFIXES = Map.ofEntries(
            Map.entry("/api/v1/payroll", "payroll.enabled"),
            Map.entry("/api/v1/trade/sales", "sales.enabled"),
            Map.entry("/api/v1/trade/procurement", "procurement.enabled"),
            Map.entry("/api/v1/manufacturing", "manufacturing.enabled"),
            Map.entry("/api/v1/finance", "finance.enabled"),
            Map.entry("/api/v1/fiscal-periods", "finance.enabled"),
            Map.entry("/api/v1/workforce/contractors", "workforce.contractorAccounts.enabled"),
            Map.entry("/api/v1/workforce/settlements", "workforce.contractorAccounts.enabled"));

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

        for (Map.Entry<String, String> gate : GATED_PREFIXES.entrySet()) {
            if (uri.startsWith(gate.getKey())
                    && !featureService.isEnabled(appId, gate.getValue())) {
                throw new BusinessRuleException("Feature is disabled", "FEATURE_DISABLED", HttpStatus.FORBIDDEN);
            }
        }

        return true;
    }
}
