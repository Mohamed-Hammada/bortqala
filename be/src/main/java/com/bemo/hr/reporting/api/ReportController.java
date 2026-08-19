package com.bemo.hr.reporting.api;

import com.bemo.hr.employee.domain.PayCycle;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.application.ReportingService;
import com.bemo.hr.shared.api.TransitionResponse;
import com.bemo.hr.shared.security.AuthService;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportingService reportingService;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    List<ReportingApi.Summary> list() {
        return reportingService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ReportingApi.Details get(@PathVariable String id) {
        return reportingService.get(id);
    }

    @GetMapping("/available-periods")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    List<ReportingApi.PeriodOption> available(@RequestParam int year) {
        return reportingService.availablePeriods(year);
    }

    @GetMapping("/preview")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ReportingApi.PreviewResponse preview(@RequestParam LocalDate periodStart, @RequestParam LocalDate periodEnd,
                                         @RequestParam PayCycle payCycle) {
        return reportingService.preview(periodStart, periodEnd, payCycle);
    }

    @GetMapping("/{id}/decision-history")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    List<ReportingApi.DecisionHistoryView> decisionHistory(@PathVariable String id) {
        return reportingService.decisionHistory(id);
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    @ResponseStatus(HttpStatus.CREATED)
    ReportingApi.Details create(@Valid @RequestBody ReportingApi.CreateRequest request, Authentication authentication) {
        return reportingService.create(request, authentication.getName());
    }

    @PostMapping("/{reportId}/bulk-decision")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ReportingApi.BulkDecisionResponse bulkDecision(@PathVariable String reportId,
                                                   @Valid @RequestBody ReportingApi.BulkDecisionRequest request,
                                                   Authentication authentication) {
        return reportingService.bulkDecide(reportId, request, authentication.getName());
    }

    @PutMapping("/{reportId}/daily-results/{resultId}/decision")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ReportingApi.Details decideDaily(@PathVariable String reportId, @PathVariable String resultId,
                                     @Valid @RequestBody ReportingApi.DecisionRequest request, Authentication authentication) {
        return reportingService.decideDaily(reportId, resultId, request, authentication.getName());
    }

    @PutMapping("/{reportId}/downtime-decision")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    ReportingApi.Details saveDowntimeDecision(@PathVariable String reportId,
                                              @Valid @RequestBody ReportingApi.DowntimeDecisionRequest request,
                                              Authentication authentication) {
        return reportingService.saveDowntimeDecision(reportId, request, authentication.getName());
    }

    @PostMapping("/{reportId}/day-anomalies/detect")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ReportingApi.Details detectDayAnomalies(@PathVariable String reportId, Authentication authentication) {
        return reportingService.detectDayAnomalies(reportId, authentication.getName());
    }

    @PostMapping("/{reportId}/day-anomalies/{anomalyId}/decision")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    ReportingApi.DayAnomalyActionResponse decideDayAnomaly(@PathVariable String reportId,
                                                           @PathVariable String anomalyId, @Valid @RequestBody ReportingApi.DayAnomalyDecisionRequest request,
                                                           Authentication authentication) {
        return reportingService.decideDayAnomaly(reportId, anomalyId, request, authentication.getName());
    }

    @PostMapping("/{reportId}/day-anomalies/{anomalyId}/reverse")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    ReportingApi.DayAnomalyActionResponse reverseDayAnomaly(@PathVariable String reportId,
                                                            @PathVariable String anomalyId, Authentication authentication) {
        return reportingService.reverseDayAnomaly(reportId, anomalyId, authentication.getName());
    }

    @PostMapping("/{reportId}/day-anomalies/{anomalyId}/reopen")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    ReportingApi.Details reopenDayAnomaly(@PathVariable String reportId, @PathVariable String anomalyId,
                                          Authentication authentication) {
        return reportingService.reopenDayAnomaly(reportId, anomalyId, authentication.getName());
    }

    @PutMapping("/{reportId}/holiday-proposals/{proposalId}/decision")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ReportingApi.Details decideHoliday(@PathVariable String reportId, @PathVariable String proposalId,
                                       @Valid @RequestBody ReportingApi.HolidayDecisionRequest request, Authentication authentication) {
        return reportingService.decideHoliday(reportId, proposalId, request, authentication.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    TransitionResponse approve(@PathVariable String id, Authentication authentication) {
        return reportingService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_MANAGER)
    TransitionResponse reopen(@PathVariable String id, Authentication authentication) {
        return reportingService.reopen(id, authentication.getName());
    }

    @GetMapping("/{id}/export")
    @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.HR_TEAM)
    ResponseEntity<byte[]> export(@PathVariable String id, Authentication authentication) {
        var preference = authService.currentPreferences(authentication.getName());
        var options = new ExcelExportOptions(preference.locale(), preference.excelTableStyle());
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String baseName = preference.locale().startsWith("ar") ? "تقرير-الحضور" : "attendance-report";
        headers.setContentDisposition(ContentDisposition.attachment().filename(baseName + "-"
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(reportingService.export(id, options), headers, HttpStatus.OK);
    }
}
