package com.bemo.hr.shared.security;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SaasTenantConfiguration {
    @Bean
    CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolver<>() {
            @Override
            public String resolveCurrentTenantIdentifier() {
                return TenantContext.currentOrSystem();
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return true;
            }
        };
    }

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer(
            CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver) {
        return properties -> properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                currentTenantIdentifierResolver);
    }
}
