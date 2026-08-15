package com.bemo.shared.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

/**
 * WebClient exchange filter that logs outbound HTTP calls (method, URL, status, duration).
 * Register via {@code WebClient.builder().filter(new ClientLoggingInterceptor())}.
 */
public class ClientLoggingInterceptor implements ExchangeFilterFunction {

    private static final Logger LOG = LoggerFactory.getLogger(ClientLoggingInterceptor.class);

    private final boolean enabled;

    public ClientLoggingInterceptor() {
        this(true);
    }

    public ClientLoggingInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (!enabled) {
            return next.exchange(request);
        }
        long start = System.nanoTime();
        return next.exchange(request).doOnNext(response -> {
            long durationMs = (System.nanoTime() - start) / 1_000_000L;
            LOG.info("Outbound HTTP [{} {}] -> {} in {} ms",
                    request.method(), request.url(), response.statusCode().value(), durationMs);
        });
    }
}
