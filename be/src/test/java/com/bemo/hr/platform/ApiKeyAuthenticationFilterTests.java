package com.bemo.hr.platform;

import com.bemo.hr.platform.application.ApiKeyService;
import com.bemo.hr.platform.domain.ApiKey;
import com.bemo.hr.platform.domain.ApiKeyRepository;
import com.bemo.hr.platform.infrastructure.ApiKeyAuthenticationFilter;
import com.bemo.hr.platform.infrastructure.ApiKeyRateLimiter;
import com.bemo.hr.shared.security.ApiKeyAuthentication;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticationFilterTests {

    private static final String APP_ID = "app-1";

    private ApiKey activeKey() {
        ApiKey key = mock(ApiKey.class);
        when(key.getId()).thenReturn("key-1");
        when(key.getAppId()).thenReturn(APP_ID);
        when(key.isActive()).thenReturn(true);
        when(key.getRateLimitPerMin()).thenReturn(1000);
        when(key.scopeSet()).thenReturn(Set.of("invoices:read", "sales:read"));
        return key;
    }

    private String fullKey() {
        return "bk_" + APP_ID + "_" + "a".repeat(32);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolderClearer.clear();
        TenantContext.clear();
    }

    private static final class SecurityContextHolderClearer {
        static void clear() {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void missingHeader_passesThroughWithoutAuth() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> during = new AtomicReference<>();
        FilterChain chain = (req, res) -> during.set(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertNull(during.get());
        assertEquals(200, response.getStatus());
        verify(repo, never()).findByAppIdAndKeyHash(any(), any());
    }

    @Test
    void validKey_withMatchingScope_authenticates() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKey key = activeKey();
        when(repo.findByAppIdAndKeyHash(APP_ID, ApiKeyService.sha256(fullKey()))).thenReturn(Optional.of(key));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> during = new AtomicReference<>();
        AtomicReference<String> tenantDuring = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            during.set(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
            tenantDuring.set(TenantContext.current());
        };

        filter.doFilter(request, response, chain);

        Authentication auth = during.get();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertTrue(auth instanceof ApiKeyAuthentication);
        assertEquals("ROLE_API_KEY", auth.getAuthorities().stream()
                .map(g -> g.getAuthority()).filter(a -> a.startsWith("ROLE_")).findFirst().orElse(null));
        assertTrue(auth.getAuthorities().stream().anyMatch(g -> "SCOPE_invoices:read".equals(g.getAuthority())));
        assertEquals(APP_ID, tenantDuring.get());
        assertEquals(200, response.getStatus());
        verify(key).setLastUsedAt(any());
    }

    @Test
    void unknownKey_returns401() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        when(repo.findByAppIdAndKeyHash(APP_ID, ApiKeyService.sha256(fullKey()))).thenReturn(Optional.empty());
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> during = new AtomicReference<>();
        FilterChain chain = (req, res) -> during.set(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("APIKEY_INVALID"));
        assertNull(during.get());
        assertNull(TenantContext.current());
    }

    @Test
    void inactiveKey_returns401() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKey key = activeKey();
        when(key.isActive()).thenReturn(false);
        when(repo.findByAppIdAndKeyHash(APP_ID, ApiKeyService.sha256(fullKey()))).thenReturn(Optional.of(key));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, null);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("APIKEY_INACTIVE"));
    }

    @Test
    void wrongScope_returns403() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKey key = activeKey();
        when(repo.findByAppIdAndKeyHash(APP_ID, ApiKeyService.sha256(fullKey()))).thenReturn(Optional.of(key));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payroll/runs");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, null);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("APIKEY_SCOPE_DENIED"));
    }

    @Test
    void exceededRateLimit_returns429WithRetryAfter() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKey key = activeKey();
        when(key.getRateLimitPerMin()).thenReturn(1);
        when(repo.findByAppIdAndKeyHash(APP_ID, ApiKeyService.sha256(fullKey()))).thenReturn(Optional.of(key));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());

        MockHttpServletRequest first = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
        first.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, (req, res) -> { });

        MockHttpServletRequest second = new MockHttpServletRequest("GET", "/api/v1/invoices/124");
        second.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, null);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertEquals("60", secondResponse.getHeader("Retry-After"));
        assertTrue(secondResponse.getContentAsString().contains("APIKEY_RATE_LIMITED"));
    }

    @Test
    void wildcardScope_grantsAnyPath() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKey key = activeKey();
        when(key.scopeSet()).thenReturn(Set.of("*"));
        when(repo.findByAppIdAndKeyHash(APP_ID, ApiKeyService.sha256(fullKey()))).thenReturn(Optional.of(key));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payroll/runs");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, fullKey());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> during = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> during.set(
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()));

        assertNotNull(during.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void malformedKey_rejected() throws Exception {
        ApiKeyRepository repo = mock(ApiKeyRepository.class);
        ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(repo, new ApiKeyRateLimiter());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/invoices/123");
        request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "not-a-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, null);

        assertEquals(401, response.getStatus());
        verify(repo, never()).findByAppIdAndKeyHash(any(), any());
    }

    @Test
    void parseAppId_handlesAppIdsWithUnderscores() {
        assertEquals("my_app_1", ApiKeyAuthenticationFilter.parseAppId("bk_my_app_1_" + "b".repeat(32)));
        assertNull(ApiKeyAuthenticationFilter.parseAppId("bk_short"));
        assertNull(ApiKeyAuthenticationFilter.parseAppId("basic_auth_thing"));
    }
}