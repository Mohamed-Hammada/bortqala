package com.bemo.hr.reporting.scheduled;

import com.bemo.hr.reporting.scheduled.application.ReportScheduleApi;
import com.bemo.hr.reporting.scheduled.application.ReportScheduleService;
import com.bemo.hr.reporting.scheduled.domain.ReportSchedule;
import com.bemo.hr.reporting.scheduled.domain.ReportScheduleRepository;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceTests {

    @Mock
    private ReportScheduleRepository reportScheduleRepository;

    @InjectMocks
    private ReportScheduleService reportScheduleService;

    private static final String TEST_APP_ID = "DEMO";

    @BeforeEach
    void setUp() {
        TenantContext.set(TEST_APP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_validRequest_persistsSchedule() {
        var request = new ReportScheduleApi.CreateRequest(
                "Daily Cash", "CASHFLOW", "{}", "EMAIL", "admin@test.com", "DAILY", "08:00");
        when(reportScheduleRepository.save(any(ReportSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = reportScheduleService.create(request);

        assertEquals("Daily Cash", result.getName());
        assertEquals(ReportSchedule.ReportKind.CASHFLOW, result.getReportKind());
        assertEquals(ReportSchedule.Channel.EMAIL, result.getChannel());
        assertEquals(ReportSchedule.Cadence.DAILY, result.getCadence());
        assertTrue(result.isActive());
        assertEquals(0, result.getConsecutiveFailures());
        verify(reportScheduleRepository).save(any(ReportSchedule.class));
    }

    @Test
    void list_returnsAllForApp() {
        var schedule = new ReportSchedule(TEST_APP_ID, "Test", ReportSchedule.ReportKind.TRENDS,
                "{}", ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.WEEKLY, "09:00");
        when(reportScheduleRepository.findByAppIdOrderByCreatedAtDesc(TEST_APP_ID)).thenReturn(List.of(schedule));

        var result = reportScheduleService.list();

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getName());
    }

    @Test
    void getById_existingId_returnsSchedule() {
        var schedule = new ReportSchedule(TEST_APP_ID, "AR Aging", ReportSchedule.ReportKind.AR_AGING,
                "{}", ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.MONTHLY, "08:00");
        when(reportScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        var result = reportScheduleService.getById(schedule.getId());

        assertEquals("AR Aging", result.getName());
    }

    @Test
    void getById_unknownId_throwsNotFound() {
        when(reportScheduleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(com.bemo.hr.shared.domain.NotFoundException.class,
                () -> reportScheduleService.getById("nonexistent"));
    }

    @Test
    void runNow_whatsappChannel_marksSkipped() {
        var schedule = new ReportSchedule(TEST_APP_ID, "WA Report", ReportSchedule.ReportKind.CUSTOM,
                "{}", ReportSchedule.Channel.WHATSAPP, "a@b.com", ReportSchedule.Cadence.DAILY, "08:00");
        when(reportScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(reportScheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = reportScheduleService.runNow(schedule.getId());

        assertEquals("SKIPPED_CHANNEL", result.getLastStatus());
        verify(reportScheduleRepository).save(any());
    }

    @Test
    void runNow_emailSuccess_marksSuccess() {
        var schedule = new ReportSchedule(TEST_APP_ID, "Cash", ReportSchedule.ReportKind.CASHFLOW,
                "{}", ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.DAILY, "08:00");
        when(reportScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(reportScheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = reportScheduleService.runNow(schedule.getId());

        assertEquals("SUCCESS", result.getLastStatus());
        assertEquals(0, result.getConsecutiveFailures());
        assertNull(result.getLastError());
    }

    @Test
    void singleRun_success_marksSuccessAndActive() {
        var schedule = new ReportSchedule(TEST_APP_ID, "Success", ReportSchedule.ReportKind.CUSTOM,
                "{}", ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.DAILY, "08:00");
        when(reportScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(reportScheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = reportScheduleService.runNow(schedule.getId());

        assertTrue(result.isActive());
        assertEquals("SUCCESS", result.getLastStatus());
        assertEquals(0, result.getConsecutiveFailures());
    }

    @Test
    void responseFrom_mapsAllFields() {
        var schedule = new ReportSchedule(TEST_APP_ID, "Test", ReportSchedule.ReportKind.PAYROLL,
                "{\"preset\":\"MONTHLY\"}", ReportSchedule.Channel.EMAIL, "admin@test.com",
                ReportSchedule.Cadence.MONTHLY, "07:30");
        schedule.markSuccess();

        var response = ReportScheduleApi.Response.from(schedule);

        assertNotNull(response.id());
        assertEquals("Test", response.name());
        assertEquals("PAYROLL", response.reportKind());
        assertEquals("{\"preset\":\"MONTHLY\"}", response.params());
        assertEquals("EMAIL", response.channel());
        assertEquals("admin@test.com", response.recipients());
        assertEquals("MONTHLY", response.cadence());
        assertEquals("07:30", response.timeOfDay());
        assertTrue(response.active());
        assertEquals("SUCCESS", response.lastStatus());
        assertEquals(0, response.consecutiveFailures());
    }

    @Test
    void update_nameOnly_preservesOtherFields() {
        var schedule = new ReportSchedule(TEST_APP_ID, "Old Name", ReportSchedule.ReportKind.CASHFLOW,
                "{}", ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.DAILY, "08:00");
        when(reportScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(reportScheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var update = new ReportScheduleApi.UpdateRequest("New Name", null, null, null, null, null, null, null);
        var result = reportScheduleService.update(schedule.getId(), update);

        assertEquals("New Name", result.getName());
        assertEquals(ReportSchedule.ReportKind.CASHFLOW, result.getReportKind());
    }

    @Test
    void delete_existingSchedule_removesFromRepo() {
        var schedule = new ReportSchedule(TEST_APP_ID, "ToDelete", ReportSchedule.ReportKind.CUSTOM,
                "{}", ReportSchedule.Channel.EMAIL, "a@b.com", ReportSchedule.Cadence.DAILY, "08:00");
        when(reportScheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        reportScheduleService.delete(schedule.getId());

        verify(reportScheduleRepository).delete(schedule);
    }
}
