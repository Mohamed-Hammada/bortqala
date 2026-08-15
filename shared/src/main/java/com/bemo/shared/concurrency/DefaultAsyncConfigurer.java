package com.bemo.shared.concurrency;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Default {@link AsyncConfigurer} used when the application declares {@code @EnableAsync}.
 * Provides a context-propagating pool sized by {@link ConcurrencyProperties} and a fail-safe
 * handler that logs uncaught async exceptions instead of swallowing them.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(ConcurrencyProperties.class)
public class DefaultAsyncConfigurer implements AsyncConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAsyncConfigurer.class);

    private final ConcurrencyProperties properties;

    public DefaultAsyncConfigurer(ConcurrencyProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ThreadPoolTaskExecutor frameworkTaskExecutor() {
        ThreadPoolTaskExecutor executor = ExecutorsFactory.newThreadPoolTaskExecutor(
                properties.threadNamePrefix(),
                properties.corePoolSize(),
                properties.maxPoolSize(),
                properties.queueCapacity());
        executor.setKeepAliveSeconds(properties.keepAliveSeconds());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return frameworkTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable throwable, Method method, Object... params) ->
                LOG.error("Uncaught exception in async method [{}]", method, throwable);
    }
}
