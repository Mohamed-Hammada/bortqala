package com.bemo.hr.product.support;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.product.analytics.ActivationMilestoneRepository;
import com.bemo.hr.product.analytics.ProductEventDailyAggregate;
import com.bemo.hr.product.analytics.ProductEventDailyAggregateRepository;
import com.bemo.hr.product.onboarding.OnboardingAssessmentRepository;
import com.bemo.hr.product.subscription.TenantSubscriptionRepository;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportHealthWindowTests {

    @Mock TenantApplicationRepository tenantRepository;
    @Mock SupportTicketRepository ticketRepository;
    @Mock SupportTicketUpdateRepository updateRepository;
    @Mock FeedbackItemRepository feedbackRepository;
    @Mock CustomerHealthSnapshotRepository healthRepository;
    @Mock ProductEventDailyAggregateRepository dailyRepository;
    @Mock ActivationMilestoneRepository milestoneRepository;
    @Mock OnboardingAssessmentRepository onboardingRepository;
    @Mock TenantSubscriptionRepository subscriptionRepository;
    @Mock AuditService auditService;

    private SupportService service;
    private TenantApplication tenant;

    @BeforeEach
    void setup() {
        tenant = new TenantApplication("support-health-window", "Support");
        TenantContext.set(tenant.getId());

        service = new SupportService(
                tenantRepository,
                ticketRepository,
                updateRepository,
                feedbackRepository,
                healthRepository,
                dailyRepository,
                milestoneRepository,
                onboardingRepository,
                subscriptionRepository,
                new ObjectMapper(),
                auditService
        );

        lenient().when(tenantRepository.findByIdForUpdate(tenant.getId())).thenReturn(Optional.of(tenant));
        lenient().when(healthRepository.findByOperationId(anyString())).thenReturn(Optional.empty());
        lenient().when(healthRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(milestoneRepository.findAllByOrderByAchievedAtAsc()).thenReturn(List.of());
        lenient().when(onboardingRepository.findFirstByOrderByAssessedAtDesc()).thenReturn(Optional.empty());
        lenient().when(ticketRepository.countByStatusNotInAndPriorityIn(any(), any())).thenReturn(0L);
        lenient().when(subscriptionRepository.findFirstBy()).thenReturn(Optional.empty());
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void oldFailuresDoNotPoisonOperationalHealthAndOldFeaturesExpireFromAdoption() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        ProductEventDailyAggregate recentUsage =
                new ProductEventDailyAggregate(today, "PAGE_VIEW", "dashboard");
        ProductEventDailyAggregate oldFailure =
                new ProductEventDailyAggregate(today.minusDays(40), "IMPORT_FAIL", "imports");
        ProductEventDailyAggregate oldFeature =
                new ProductEventDailyAggregate(today.minusDays(45), "PAGE_VIEW", "old-feature");

        when(dailyRepository.findAllByOrderByEventDateDesc())
                .thenReturn(List.of(recentUsage, oldFailure, oldFeature));

        SupportApi.HealthResponse response =
                service.calculate(new SupportApi.HealthRequest("health-window-1"), "admin");

        assertThat(response.dimensions().get("operationalHealth")).isEqualTo(10);
        assertThat(response.dimensions().get("workflowAdoption")).isEqualTo(4);
    }
}
