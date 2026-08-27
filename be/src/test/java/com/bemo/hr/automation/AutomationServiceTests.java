package com.bemo.hr.automation;

import com.bemo.hr.automation.api.AutomationApi;
import com.bemo.hr.automation.application.AutomationService;
import com.bemo.hr.automation.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomationServiceTests {

    @Mock RecurringTemplateRepository templateRepo;
    @Mock DunningRuleRepository dunningRepo;

    AutomationService service;

    @BeforeEach
    void setUp() {
        service = new AutomationService(templateRepo, dunningRepo);
    }

    @Test
    void createTemplate_persistsAndReturns() {
        when(templateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var payload = new AutomationApi.RecurringTemplatePayload(
                "INVOICE", "Monthly Rent", "{\"amount\":5000}", "MONTHLY", null,
                Instant.now().plusSeconds(86400).toEpochMilli());
        var result = service.createTemplate("app-1", payload);
        assertNotNull(result.id());
        assertEquals("INVOICE", result.kind());
        assertEquals("Monthly Rent", result.templateName());
        assertTrue(result.active());
    }

    @Test
    void runRecurringTemplates_advancesNextRun() {
        RecurringTemplate tpl = new RecurringTemplate("app-1", RecurringTemplate.Kind.INVOICE,
                "Rent", "{}", RecurringTemplate.Cadence.MONTHLY, null,
                Instant.now().minusSeconds(3600));
        when(templateRepo.findByAppIdAndActiveTrue("app-1")).thenReturn(List.of(tpl));
        when(templateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        int created = service.runRecurringTemplates("app-1");
        assertEquals(1, created);
        assertNotNull(tpl.getLastCreatedRef());
    }

    @Test
    void runRecurringTemplates_skipsFutureRuns() {
        RecurringTemplate tpl = new RecurringTemplate("app-1", RecurringTemplate.Kind.INVOICE,
                "Rent", "{}", RecurringTemplate.Cadence.MONTHLY, null,
                Instant.now().plusSeconds(86400));
        when(templateRepo.findByAppIdAndActiveTrue("app-1")).thenReturn(List.of(tpl));
        int created = service.runRecurringTemplates("app-1");
        assertEquals(0, created);
    }

    @Test
    void createDunningRule_persists() {
        when(dunningRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var payload = new AutomationApi.DunningRulePayload(15, "dunning_15d", "WHATSAPP");
        var result = service.createDunningRule("app-1", payload);
        assertEquals(15, result.daysOverdue());
        assertEquals("WHATSAPP", result.channel());
    }

    @Test
    void toggleTemplate_notFound_throws() {
        when(templateRepo.findById("bad")).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class,
                () -> service.toggleTemplate("app-1", "bad", true));
    }

    @Test
    void runDunning_returnsActiveRuleCount() {
        when(dunningRepo.findByAppIdAndActiveTrueOrderByDaysOverdueAsc("app-1"))
                .thenReturn(List.of(
                        new DunningRule("app-1", 15, "d15", "WHATSAPP"),
                        new DunningRule("app-1", 30, "d30", "WHATSAPP")));
        int evaluated = service.runDunning("app-1");
        assertEquals(2, evaluated);
    }

    @Test
    void getJobsHealth_returnsEmpty() {
        var result = service.getJobsHealth("app-1", null);
        assertEquals(0, result.total());
    }
}
