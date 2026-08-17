package com.bemo.hr.product.analytics;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductEventSink {
    private static final Logger log = LoggerFactory.getLogger(ProductEventSink.class);
    private final ObjectProvider<ProductAnalyticsService> serviceProvider;

    public boolean recordSafely(ProductAnalyticsApi.EventRequest request, String actor) {
        try {
            serviceProvider.getObject().record(request, actor);
            return true;
        } catch (Exception ex) {
            log.warn("Product analytics event dropped name={} reason={}", request.eventName(), ex.getMessage());
            return false;
        }
    }
}
