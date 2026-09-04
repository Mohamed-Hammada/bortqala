package com.bemo.hr.product.analytics;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductAnalyticsServiceTests {
    @Mock
    TenantApplicationRepository tenantRepository;
    @Mock
    ProductEventRepository eventRepository;
    @Mock
    ProductEventDailyAggregateRepository dailyRepository;
    @Mock
    ActivationMilestoneRepository milestoneRepository;
    @Mock
    JdbcTemplate jdbcTemplate;
    @Mock
    AuditService auditService;
    @Mock
    PlatformTransactionManager transactionManager;
    @Mock
    TransactionStatus transactionStatus;
    ProductAnalyticsService service;
    TenantApplication tenant;

    @BeforeEach
    void setup() {
        tenant = new TenantApplication("analytics", "Analytics");
        TenantContext.set(tenant.getId());
        service = new ProductAnalyticsService(tenantRepository, eventRepository, dailyRepository, milestoneRepository, new ObjectMapper(), jdbcTemplate, auditService, transactionManager);
        lenient().when(tenantRepository.findByIdForUpdate(tenant.getId())).thenReturn(Optional.of(tenant));
        lenient().when(eventRepository.findByOperationId(anyString())).thenReturn(Optional.empty());
        lenient().when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(dailyRepository.findByEventDateAndEventNameAndFeatureKey(any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(dailyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void allowlistedEventIsIdempotentAggregatedAndActivatesMilestone() {
        var request = new ProductAnalyticsApi.EventRequest("IMPORT_COMPLETED", "workforce.imports", "op-1", Map.of("result", "success", "count", 10));
        var first = service.record(request, "admin");
        assertThat(first.replay()).isFalse();
        verify(dailyRepository).save(any(ProductEventDailyAggregate.class));
        verify(milestoneRepository).save(argThat(m -> m.getMilestoneKey().equals("FIRST_IMPORT")));
        var event = new ProductEvent("IMPORT_COMPLETED", "workforce.imports", "{}", "op-1", "admin");
        when(eventRepository.findByOperationId("op-1")).thenReturn(Optional.of(event));
        assertThat(service.record(request, "admin").replay()).isTrue();
        verify(eventRepository, times(1)).save(any());
    }

    @Test
    void sensitiveOrStructuredPropertiesAreRejected() {
        var sensitive = new ProductAnalyticsApi.EventRequest("PAGE_VIEW", "navigation", "op", Map.of("email", "person@example.com"));
        assertThatThrownBy(() -> service.record(sensitive, "admin")).isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("PRODUCT_EVENT_PROPERTY_NOT_ALLOWED"));
        var structured = new ProductAnalyticsApi.EventRequest("PAGE_VIEW", "navigation", "op2", Map.of("source", Map.of("nested", true)));
        assertThatThrownBy(() -> service.record(structured, "admin")).isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("PRODUCT_EVENT_PROPERTIES_INVALID"));
        verifyNoInteractions(eventRepository);
    }

    @Test
    void tenantSummaryUsesDurableAggregatesAfterRawRetention() {
        var daily = new ProductEventDailyAggregate(LocalDate.now(), "PAGE_VIEW", "navigation");
        daily.increment();
        when(dailyRepository.findAllByOrderByEventDateDesc()).thenReturn(List.of(daily));
        var milestone = new ActivationMilestone("FIRST_LOGIN", "event", java.time.Instant.now());
        when(milestoneRepository.findAllByOrderByAchievedAtAsc()).thenReturn(List.of(milestone));
        var result = service.summary();
        assertThat(result.eventCount()).isEqualTo(2);
        assertThat(result.activeDays()).isEqualTo(1);
        assertThat(result.activationScore()).isEqualTo(16);
        assertThat(result.features()).extracting(ProductAnalyticsApi.FeatureUsage::featureKey).containsExactly("navigation");
    }

    @Test
    void safeSinkSwallowsAnalyticsFailureOutsideErpTransaction() {
        ProductAnalyticsService failing = mock(ProductAnalyticsService.class);
        @SuppressWarnings("unchecked") ObjectProvider<ProductAnalyticsService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(failing);
        doThrow(new RuntimeException("database unavailable")).when(failing).record(any(), any());
        var sink = new ProductEventSink(provider);
        assertThat(sink.recordSafely(new ProductAnalyticsApi.EventRequest("PAGE_VIEW", "navigation", "op", Map.of()), "admin")).isFalse();
    }

    @Test
    void platformAnalyticsRequiresExplicitSuperAdminPermission() throws Exception {
        var method = ProductAnalyticsController.class.getDeclaredMethod("platform");
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('SUPER_ADMIN')");
    }
}
