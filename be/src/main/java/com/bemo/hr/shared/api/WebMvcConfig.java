package com.bemo.hr.shared.api;

import com.bemo.hr.shared.security.TenantFeatureService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantFeatureService tenantFeatureService;
    private final com.bemo.hr.shared.security.EntitlementCatalog entitlementCatalog;
    private final com.bemo.hr.product.trial.TrialDemoService trialDemoService;

    public WebMvcConfig(TenantFeatureService tenantFeatureService, com.bemo.hr.shared.security.EntitlementCatalog entitlementCatalog, com.bemo.hr.product.trial.TrialDemoService trialDemoService) {
        this.tenantFeatureService = tenantFeatureService;
        this.entitlementCatalog = entitlementCatalog;
        this.trialDemoService = trialDemoService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantFeatureInterceptor(tenantFeatureService, entitlementCatalog))
                .addPathPatterns("/api/v1/**");
        registry.addInterceptor(new TrialWriteInterceptor(trialDemoService)).addPathPatterns("/api/v1/**");
    }
}
