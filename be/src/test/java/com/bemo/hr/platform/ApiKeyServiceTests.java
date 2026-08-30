package com.bemo.hr.platform;

import com.bemo.hr.platform.api.PlatformApi;
import com.bemo.hr.platform.application.ApiKeyService;
import com.bemo.hr.platform.domain.ApiKey;
import com.bemo.hr.platform.domain.ApiKeyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTests {

    @Mock ApiKeyRepository apiKeyRepo;
    ApiKeyService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyService(apiKeyRepo);
    }

    @Test
    void createKey_persistsAndReturnsFullKey() {
        when(apiKeyRepo.findByAppIdOrderByCreatedAtDesc("app-1")).thenReturn(List.of());
        when(apiKeyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new PlatformApi.ApiKeyCreateRequest("Test Key", "invoices:read", 60);
        var result = service.createKey("app-1", req, "admin");

        assertNotNull(result.id());
        assertTrue(result.fullKey().startsWith("bk_"));
        assertEquals("invoices:read", result.scopes());
        assertEquals(60, result.rateLimitPerMin());
        assertTrue(result.active());

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepo).save(captor.capture());
        assertEquals(ApiKeyService.sha256(result.fullKey()), captor.getValue().getKeyHash());
        assertEquals("app-1", com.bemo.hr.platform.infrastructure.ApiKeyAuthenticationFilter.parseAppId(result.fullKey()));
    }

    @Test
    void createKey_defaultRateLimit() {
        when(apiKeyRepo.findByAppIdOrderByCreatedAtDesc("app-1")).thenReturn(List.of());
        when(apiKeyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new PlatformApi.ApiKeyCreateRequest("Key", null, 0);
        var result = service.createKey("app-1", req, "admin");
        assertEquals(120, result.rateLimitPerMin());
    }

    @Test
    void createKey_limitReached_throws() {
        List<ApiKey> existing = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> mock(ApiKey.class))
                .toList();
        when(apiKeyRepo.findByAppIdOrderByCreatedAtDesc("app-1")).thenReturn(existing);

        var req = new PlatformApi.ApiKeyCreateRequest("Key", "", 120);
        assertThrows(BusinessRuleException.class, () -> service.createKey("app-1", req, "admin"));
    }

    @Test
    void listKeys_returnsDescOrder() {
        ApiKey k1 = mock(ApiKey.class);
        ApiKey k2 = mock(ApiKey.class);
        when(apiKeyRepo.findByAppIdOrderByCreatedAtDesc("app-1")).thenReturn(List.of(k1, k2));
        when(k1.getId()).thenReturn("id-1");
        when(k1.getName()).thenReturn("Key 1");
        when(k1.getScopes()).thenReturn("");
        when(k1.getRateLimitPerMin()).thenReturn(120);
        when(k1.isActive()).thenReturn(true);
        when(k1.getLastUsedAt()).thenReturn(null);
        when(k1.getCreatedBy()).thenReturn("admin");
        when(k1.getCreatedAt()).thenReturn(java.time.Instant.now());
        when(k1.getUpdatedAt()).thenReturn(java.time.Instant.now());
        when(k1.getVersion()).thenReturn(1L);

        when(k2.getId()).thenReturn("id-2");
        when(k2.getName()).thenReturn("Key 2");
        when(k2.getScopes()).thenReturn("employees:read");
        when(k2.getRateLimitPerMin()).thenReturn(60);
        when(k2.isActive()).thenReturn(false);
        when(k2.getLastUsedAt()).thenReturn(java.time.Instant.now());
        when(k2.getCreatedBy()).thenReturn("admin");
        when(k2.getCreatedAt()).thenReturn(java.time.Instant.now());
        when(k2.getUpdatedAt()).thenReturn(java.time.Instant.now());
        when(k2.getVersion()).thenReturn(2L);

        var result = service.listKeys("app-1");
        assertEquals(2, result.keys().size());
    }

    @Test
    void toggleKey_notFound_throws() {
        when(apiKeyRepo.findById("bad")).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> service.toggleKey("app-1", "bad", false));
    }

    @Test
    void toggleKey_wrongApp_throws() {
        ApiKey key = mock(ApiKey.class);
        when(apiKeyRepo.findById("id-1")).thenReturn(Optional.of(key));
        when(key.getAppId()).thenReturn("other-app");
        assertThrows(BusinessRuleException.class, () -> service.toggleKey("app-1", "id-1", false));
    }

    @Test
    void revokeKey_deactivates() {
        ApiKey key = mock(ApiKey.class);
        when(apiKeyRepo.findById("id-1")).thenReturn(Optional.of(key));
        when(key.getAppId()).thenReturn("app-1");
        service.revokeKey("app-1", "id-1");
        verify(key).setActive(false);
    }

    @Test
    void sha256_deterministic() {
        String h1 = ApiKeyService.sha256("test-key");
        String h2 = ApiKeyService.sha256("test-key");
        assertEquals(h1, h2);
        assertTrue(h1.length() == 64);
    }
}
