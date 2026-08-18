package com.bemo.hr.product.analytics;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAnalyticsService {
    private static final Set<String> ALLOWED = Set.of("source", "result", "count", "durationMs", "route", "objectType");
    private static final Map<String, String> MILESTONES = Map.of("LOGIN", "FIRST_LOGIN", "IMPORT_COMPLETED", "FIRST_IMPORT", "REPORT_APPROVED", "FIRST_REPORT_APPROVED", "PAYROLL_POSTED", "FIRST_PAYROLL_POSTED", "INVOICE_ISSUED", "FIRST_INVOICE", "BANK_RECONCILED", "FIRST_RECONCILIATION");
    private final TenantApplicationRepository tenantRepository;
    private final ProductEventRepository eventRepository;
    private final ProductEventDailyAggregateRepository dailyRepository;
    private final ActivationMilestoneRepository milestoneRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AuditService auditService;

    private static long epoch(Timestamp value) {
        return value == null ? 0 : value.toInstant().toEpochMilli();
    }

    private static BusinessRuleException error(String code, HttpStatus status) {
        return new BusinessRuleException(code, code, status);
    }

    @Transactional
    public ProductAnalyticsApi.EventResponse record(ProductAnalyticsApi.EventRequest request, String actor) {
        log.debug("record called with eventName={}, featureKey={}", request.eventName(), request.featureKey());
        validate(request.properties());
        String app = TenantContext.require();
        tenantRepository.findByIdForUpdate(app).orElseThrow(() -> error("TENANT_NOT_FOUND", HttpStatus.NOT_FOUND));
        var replay = eventRepository.findByOperationId(request.operationId());
        if (replay.isPresent()) return eventView(replay.get(), true);
        String json;
        try {
            json = objectMapper.writeValueAsString(request.properties() == null ? Map.of() : request.properties());
        } catch (Exception ex) {
            log.error("Failed to serialize event properties", ex);
            throw error("PRODUCT_EVENT_PROPERTIES_INVALID", HttpStatus.BAD_REQUEST);
        }
        ProductEvent event;
        try {
            event = eventRepository.save(new ProductEvent(request.eventName(), request.featureKey(), json, request.operationId(), actor));
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate event detected for operationId={}, checking race condition", request.operationId());
            var race = eventRepository.findByOperationId(request.operationId());
            if (race.isPresent()) return eventView(race.get(), true);
            throw ex;
        }
        LocalDate day = LocalDate.ofInstant(event.getOccurredAt(), ZoneOffset.UTC);
        var aggregate = dailyRepository.findByEventDateAndEventNameAndFeatureKey(day, event.getEventName(), event.getFeatureKey()).orElse(null);
        if (aggregate == null)
            dailyRepository.save(new ProductEventDailyAggregate(day, event.getEventName(), event.getFeatureKey()));
        else {
            aggregate.increment();
            dailyRepository.save(aggregate);
        }
        String milestone = MILESTONES.get(event.getEventName());
        if (milestone != null && !milestoneRepository.existsByMilestoneKey(milestone)) {
            milestoneRepository.save(new ActivationMilestone(milestone, event.getId(), event.getOccurredAt()));
            log.info("Milestone {} achieved for event {}", milestone, event.getEventName());
        }
        return eventView(event, false);
    }

    @Transactional(readOnly = true)
    public ProductAnalyticsApi.TenantSummary summary() {
        log.debug("summary called");
        var daily = dailyRepository.findAllByOrderByEventDateDesc();
        long events = daily.stream().mapToLong(ProductEventDailyAggregate::getEventCount).sum();
        long days = daily.stream().map(ProductEventDailyAggregate::getEventDate).distinct().count();
        var milestones = milestoneRepository.findAllByOrderByAchievedAtAsc();
        var features = daily.stream().collect(Collectors.groupingBy(ProductEventDailyAggregate::getFeatureKey, Collectors.summingLong(ProductEventDailyAggregate::getEventCount))).entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).map(e -> new ProductAnalyticsApi.FeatureUsage(e.getKey(), e.getValue())).toList();
        return new ProductAnalyticsApi.TenantSummary(events, days, milestones.size() * 100 / MILESTONES.size(), milestones.stream().map(m -> new ProductAnalyticsApi.MilestoneResponse(m.getMilestoneKey(), m.getAchievedAt().toEpochMilli())).toList(), features);
    }

    @Transactional
    public ProductAnalyticsApi.RetentionResponse retain(ProductAnalyticsApi.RetentionRequest request, String actor) {
        log.debug("retain called with retainDays={}", request.retainDays());
        String app = TenantContext.require();
        tenantRepository.findByIdForUpdate(app).orElseThrow(() -> error("TENANT_NOT_FOUND", HttpStatus.NOT_FOUND));
        int deleted = eventRepository.deleteRawBefore(app, Instant.now().minus(request.retainDays(), java.time.temporal.ChronoUnit.DAYS));
        auditService.record("RETAIN", "PRODUCT_ANALYTICS", app, actor, "{\"days\":" + request.retainDays() + ",\"deleted\":" + deleted + "}", null);
        log.info("Product analytics retained {} days, {} records deleted for app={}", request.retainDays(), deleted, app);
        return new ProductAnalyticsApi.RetentionResponse(deleted, request.retainDays());
    }

    @Transactional(readOnly = true)
    public List<ProductAnalyticsApi.PlatformTenantSummary> platform() {
        log.debug("platform called");
        return jdbcTemplate.query("select a.id,a.code,a.name,coalesce(d.events,0),coalesce(m.milestones,0),d.last_event from apps a left join (select app_id,sum(event_count) events,max(updated_at) last_event from product_event_daily_aggregates group by app_id) d on d.app_id=a.id left join (select app_id,count(*) milestones from activation_milestones group by app_id) m on m.app_id=a.id order by a.code", (rs, n) -> new ProductAnalyticsApi.PlatformTenantSummary(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5), epoch(rs.getTimestamp(6))));
    }

    private void validate(Map<String, Object> properties) {
        if (properties == null) return;
        for (var e : properties.entrySet()) {
            if (!ALLOWED.contains(e.getKey())) {
                log.warn("Validation failed: property '{}' is not allowed", e.getKey());
                throw error("PRODUCT_EVENT_PROPERTY_NOT_ALLOWED", HttpStatus.BAD_REQUEST);
            }
            Object value = e.getValue();
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean) || value.toString().length() > 200) {
                log.warn("Validation failed: property '{}' has invalid value", e.getKey());
                throw error("PRODUCT_EVENT_PROPERTIES_INVALID", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private ProductAnalyticsApi.EventResponse eventView(ProductEvent e, boolean replay) {
        return new ProductAnalyticsApi.EventResponse(e.getId(), e.getEventName(), e.getFeatureKey(), e.getOccurredAt().toEpochMilli(), replay);
    }
}
