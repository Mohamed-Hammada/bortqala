package com.bemo.hr.finance.application;

import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateHintScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateHintScheduler.class);

    private final TenantApplicationRepository tenantApplicationRepository;
    private final ExchangeRateHintService exchangeRateHintService;

    public ExchangeRateHintScheduler(
            TenantApplicationRepository tenantApplicationRepository,
            ExchangeRateHintService exchangeRateHintService) {
        this.tenantApplicationRepository = tenantApplicationRepository;
        this.exchangeRateHintService = exchangeRateHintService;
    }

    @Scheduled(
            fixedDelayString = "${hr.exchange-rate.scheduler-scan-ms:300000}",
            initialDelayString = "${hr.exchange-rate.scheduler-initial-delay-ms:60000}"
    )
    public void refreshDueTenants() {
        tenantApplicationRepository.findAll().stream()
                .filter(app -> app.isActive())
                .forEach(app -> {
                    TenantContext.set(app.getId());
                    try {
                        // TenantContext is bound before the transactional service is entered.
                        exchangeRateHintService.refreshIfDue();
                    } catch (RuntimeException ex) {
                        log.warn("Exchange-rate scheduler failed for tenant {}", app.getCode(), ex);
                    } finally {
                        TenantContext.clear();
                    }
                });
    }
}
