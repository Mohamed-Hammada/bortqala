package com.bemo.hr.shared.security;

import com.bemo.hr.employee.application.DemoReferenceDataService;
import com.bemo.hr.operations.DemoScenarioDataService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "demo"})
@ConditionalOnProperty(name = "hr.bootstrap.demo-data", havingValue = "true")
@Order(20)
public class DemoDataInitializer implements ApplicationRunner {
    private final TenantApplicationRepository tenantApplicationRepository;
    private final DemoReferenceDataService demoReferenceDataService;
    private final DemoScenarioDataService demoScenarioDataService;
    private final String appCode;

    public DemoDataInitializer(
            TenantApplicationRepository tenantApplicationRepository,
            DemoReferenceDataService demoReferenceDataService,
            DemoScenarioDataService demoScenarioDataService,
            org.springframework.core.env.Environment environment) {
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.demoReferenceDataService = demoReferenceDataService;
        this.demoScenarioDataService = demoScenarioDataService;
        this.appCode = environment.getRequiredProperty("hr.bootstrap.app-code");
    }

    @Override
    public void run(ApplicationArguments args) {
        var app = tenantApplicationRepository.findByCodeIgnoreCaseAndActiveTrue(appCode)
                .orElseThrow(() -> new IllegalStateException("Bootstrap application was not created: " + appCode));
        TenantContext.set(app.getId());
        try {
            demoReferenceDataService.ensureReferenceConfiguration();
            demoScenarioDataService.ensureDemoScenarios();
        } finally {
            TenantContext.clear();
        }
    }
}
