package com.bemo.shared.config;

import java.util.List;

import com.bemo.shared.filter.MdcFilter;
import com.bemo.shared.filter.RequestPerformanceLoggingFilter;
import com.bemo.shared.filter.ServerLoggingFilter;
import com.bemo.shared.filter.TracingFilter;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;

/**
 * The single entry point for the framework's request-observability stack. Imported by
 * {@link com.bemo.shared.annotation.EnablePerformanceMetricsLogging}.
 *
 * <p>Registers, in order: correlation-id tracing, MDC context propagation, structured access
 * logging and per-request timing.</p>
 */
@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:shared.properties")
@Import(LatencyLoggingConfiguration.class)
public class PerformanceMetricsLoggingConfiguration {

    @Bean
    public TracingFilter tracingFilter(
            @Value("${shared.tracing.default.enabled:true}") boolean enabled,
            @Value("${shared.tracing.default.header:web-correlation-id}") String header) {
        return new TracingFilter(enabled, header);
    }

    @Bean
    public MdcFilter mdcFilter() {
        return new MdcFilter();
    }

    @Bean
    public ServerLoggingFilter serverLoggingFilter(
            @Value("${shared.logging.enabled:true}") boolean enabled,
            @Value("${shared.logging.excluded-headers:X-Api-Key,Authorization,X-Auth-Token,X-Device-Hub-Key,Cookie}") String excludedHeaders) {
        List<String> excluded = excludedHeaders == null || excludedHeaders.isBlank()
                ? List.of()
                : List.of(excludedHeaders.split(",")).stream().map(String::trim).filter(s -> !s.isBlank()).toList();
        return new ServerLoggingFilter(enabled, excluded);
    }

    @Bean
    public RequestPerformanceLoggingFilter requestPerformanceLoggingFilter(ObjectProvider<MeterRegistry> registryProvider,
                                                                          @Value("${shared.performance-logging.default.min-duration-in-millis:50}") long slowThreshold) {
        return new RequestPerformanceLoggingFilter(registryProvider.getIfAvailable(), slowThreshold);
    }

    @Bean
    public FilterRegistrationBean<TracingFilter> tracingFilterRegistration(TracingFilter filter) {
        return orderedRegistration(filter, Ordered.HIGHEST_PRECEDENCE);
    }

    @Bean
    public FilterRegistrationBean<MdcFilter> mdcFilterRegistration(MdcFilter filter) {
        return orderedRegistration(filter, Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Bean
    public FilterRegistrationBean<ServerLoggingFilter> serverLoggingFilterRegistration(ServerLoggingFilter filter) {
        return orderedRegistration(filter, Ordered.HIGHEST_PRECEDENCE + 2);
    }

    @Bean
    public FilterRegistrationBean<RequestPerformanceLoggingFilter> requestPerformanceLoggingFilterRegistration(RequestPerformanceLoggingFilter filter) {
        return orderedRegistration(filter, Ordered.HIGHEST_PRECEDENCE + 3);
    }

    private <F extends jakarta.servlet.Filter> FilterRegistrationBean<F> orderedRegistration(F filter, int order) {
        FilterRegistrationBean<F> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(order);
        return registration;
    }
}
