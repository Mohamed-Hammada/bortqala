package com.bemo.hr.shared.api;

import com.bemo.hr.shared.security.TenantFeatureService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantFeatureService tenantFeatureService;

    public WebMvcConfig(TenantFeatureService tenantFeatureService) {
        this.tenantFeatureService = tenantFeatureService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantFeatureInterceptor(tenantFeatureService))
                .addPathPatterns("/api/v1/**");
    }
}
