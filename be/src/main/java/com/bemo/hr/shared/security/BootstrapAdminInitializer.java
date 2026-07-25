package com.bemo.hr.shared.security;

import com.bemo.hr.employee.application.DemoReferenceDataService;
import com.bemo.hr.operations.DemoScenarioDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final AuthService authService;
    private final DemoReferenceDataService demoReferenceDataService;
    private final DemoScenarioDataService demoScenarioDataService;
    private final String username;
    private final String password;
    private final String appCode;
    private final String appName;
    private final boolean seedDemoData;

    public BootstrapAdminInitializer(AuthService authService, DemoReferenceDataService demoReferenceDataService,
                                     DemoScenarioDataService demoScenarioDataService,
                                     @Value("${hr.bootstrap.admin-username:}") String username,
                                     @Value("${hr.bootstrap.admin-password:}") String password,
                                     @Value("${hr.bootstrap.app-code:DEMO}") String appCode,
                                     @Value("${hr.bootstrap.app-name:Bemo Demo Company}") String appName,
                                     @Value("${hr.bootstrap.demo-data:false}") boolean seedDemoData) {
        this.authService = authService;
        this.demoReferenceDataService = demoReferenceDataService;
        this.demoScenarioDataService = demoScenarioDataService;
        this.username = username;
        this.password = password;
        this.appCode = appCode;
        this.appName = appName;
        this.seedDemoData = seedDemoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!username.isBlank() && !password.isBlank()) {
            var admin = authService.ensureBootstrapAppAdmin(appCode, appName, username, password);
            TenantContext.set(admin.getAppId());
            try {
                demoReferenceDataService.ensureReferenceConfiguration();
                if (seedDemoData) demoScenarioDataService.ensureDemoScenarios();
            } finally {
                TenantContext.clear();
            }
        }
    }
}
