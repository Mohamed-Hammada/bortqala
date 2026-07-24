package com.bemo.hr.reporting.api;

import com.bemo.hr.reporting.application.ReportingService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportingService reportingService;
    public ReportController(ReportingService reportingService) { this.reportingService = reportingService; }

    @GetMapping List<ReportingApi.Summary> list() { return reportingService.list(); }
    @GetMapping("/{id}") ReportingApi.Details get(@PathVariable String id) { return reportingService.get(id); }
    @GetMapping("/available-periods") List<ReportingApi.PeriodOption> available(@RequestParam int year) { return reportingService.availablePeriods(year); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    @ResponseStatus(HttpStatus.CREATED)
    ReportingApi.Details create(@Valid @RequestBody ReportingApi.CreateRequest request, Authentication authentication) {
        return reportingService.create(request, authentication.getName());
    }

    @PutMapping("/{reportId}/daily-results/{resultId}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    ReportingApi.Details decideDaily(@PathVariable String reportId, @PathVariable String resultId,
                                     @Valid @RequestBody ReportingApi.DecisionRequest request, Authentication authentication) {
        return reportingService.decideDaily(reportId, resultId, request, authentication.getName());
    }

    @PutMapping("/{reportId}/holiday-proposals/{proposalId}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
    ReportingApi.Details decideHoliday(@PathVariable String reportId, @PathVariable String proposalId,
                                       @Valid @RequestBody ReportingApi.HolidayDecisionRequest request, Authentication authentication) {
        return reportingService.decideHoliday(reportId, proposalId, request, authentication.getName());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    ReportingApi.Details approve(@PathVariable String id, Authentication authentication) { return reportingService.approve(id, authentication.getName()); }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    ReportingApi.Details reopen(@PathVariable String id) { return reportingService.reopen(id); }

    @GetMapping("/{id}/export")
    ResponseEntity<byte[]> export(@PathVariable String id) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("attendance-report-" + id + ".xlsx", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(reportingService.export(id), headers, HttpStatus.OK);
    }
}
