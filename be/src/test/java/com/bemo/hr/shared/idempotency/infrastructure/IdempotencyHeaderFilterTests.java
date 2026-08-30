package com.bemo.hr.shared.idempotency.infrastructure;

import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdempotencyHeaderFilterTests {

    private IdempotencyKeyRepository repository;
    private IdempotencyHeaderFilter filter;

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyKeyRepository.class);
        filter = new IdempotencyHeaderFilter(repository);
    }

    @Test
    void bypassesWhenNoHeaderPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/finance/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(repository);
    }

    @Test
    void bypassesForGetRequestsEvenWithHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/finance/payments");
        request.addHeader("Idempotency-Key", "KEY-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
        verifyNoInteractions(repository);
    }

    @Test
    void replaysCompletedResponseOnExactKeyAndHash() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/finance/payments");
        request.addHeader("Idempotency-Key", "KEY-123");
        request.setContent("{\"amount\":100}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        String appId = com.bemo.hr.shared.security.TenantContext.currentOrSystem();
        String hash = com.bemo.hr.shared.idempotency.application.IdempotencyService.hash(appId + "|POST|/api/v1/finance/payments|{\"amount\":100}");
        IdempotencyKey existing = new IdempotencyKey("HTTP:POST:/api/v1/finance/payments", "KEY-123", hash);
        existing.complete("201||{\"id\":\"pay-1\",\"amount\":100}");

        when(repository.findByOperationTypeAndOperationId(eq("HTTP:POST:/api/v1/finance/payments"), eq("KEY-123")))
                .thenReturn(Optional.of(existing));

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeader("X-Idempotency-Replayed")).isEqualTo("true");
        assertThat(response.getContentAsString()).contains("\"id\":\"pay-1\"");
    }

    @Test
    void rejectsWith409WhenOperationIsInProgress() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/finance/payments");
        request.addHeader("Idempotency-Key", "KEY-123");
        request.setContent("{\"amount\":100}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(repository.findByOperationTypeAndOperationId(eq("HTTP:POST:/api/v1/finance/payments"), eq("KEY-123")))
                .thenReturn(Optional.empty());
        when(repository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_CONFLICT);
        assertThat(response.getContentAsString()).contains("IDEMPOTENCY_IN_PROGRESS");
    }
}
