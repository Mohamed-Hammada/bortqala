package com.bemo.shared.config;

import com.bemo.shared.concurrency.AsyncContextSwitcher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the MDC/context propagation helper for asynchronous work.
 */
@Configuration(proxyBeanMethods = false)
public class ConcurrencyConfiguration {

    @Bean
    public AsyncContextSwitcher asyncContextSwitcher() {
        return new AsyncContextSwitcher();
    }
}
