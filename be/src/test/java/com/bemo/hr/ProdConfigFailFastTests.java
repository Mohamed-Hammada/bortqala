package com.bemo.hr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProdConfigFailFastTests {

    @Test
    void prodProfileFailsFastWhenRequiredSecretsAreMissing() {
        var application = new SpringApplication(BemoErpApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        var environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.setActiveProfiles("prod");
        application.setEnvironment(environment);
        application.setDefaultProperties(Map.of(
                "spring.liquibase.enabled", "false",
                "spring.jpa.hibernate.ddl-auto", "none",
                "spring.main.banner-mode", "off"
        ));

        var thrown = org.assertj.core.api.Assertions.catchThrowable(application::run);
        assertThat(thrown).isNotNull();
        assertThat(thrown.getLocalizedMessage())
                .containsAnyOf(
                        "'url' must start with \"jdbc\"",
                        "HR_JWT_SECRET must contain at least 32 bytes",
                        "Could not resolve placeholder");
    }
}
