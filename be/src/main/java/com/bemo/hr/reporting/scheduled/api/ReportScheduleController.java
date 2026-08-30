package com.bemo.hr.reporting.scheduled.api;

import com.bemo.hr.reporting.scheduled.application.ReportScheduleApi;
import com.bemo.hr.reporting.scheduled.application.ReportScheduleService;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-schedules")
@PreAuthorize("@auth.hasAnyPermission('reports.read', 'reports.manage', 'settings.manage')")
@RequiredArgsConstructor
public class ReportScheduleController {

    private final ReportScheduleService reportScheduleService;

    @GetMapping
    ResponseEntity<List<ReportScheduleApi.Response>> list() {
        return ResponseEntity.ok(reportScheduleService.list().stream()
                .map(ReportScheduleApi.Response::from).toList());
    }

    @GetMapping("/{id}")
    ResponseEntity<ReportScheduleApi.Response> get(@PathVariable String id) {
        return ResponseEntity.ok(ReportScheduleApi.Response.from(reportScheduleService.getById(id)));
    }

    @PostMapping
    ResponseEntity<ReportScheduleApi.Response> create(@Valid @RequestBody ReportScheduleApi.CreateRequest request) {
        return ResponseEntity.ok(ReportScheduleApi.Response.from(reportScheduleService.create(request)));
    }

    @PutMapping("/{id}")
    ResponseEntity<ReportScheduleApi.Response> update(@PathVariable String id,
                                                       @Valid @RequestBody ReportScheduleApi.UpdateRequest request) {
        return ResponseEntity.ok(ReportScheduleApi.Response.from(reportScheduleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) {
        reportScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run-now")
    ResponseEntity<ReportScheduleApi.Response> runNow(@PathVariable String id) {
        return ResponseEntity.ok(ReportScheduleApi.Response.from(reportScheduleService.runNow(id)));
    }
}
