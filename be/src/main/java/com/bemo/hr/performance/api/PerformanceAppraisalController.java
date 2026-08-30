package com.bemo.hr.performance.api;

import com.bemo.hr.performance.application.PerformanceAppraisalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/performance")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'HR_REVIEWER')")
public class PerformanceAppraisalController {

    private final PerformanceAppraisalService performanceService;

    public PerformanceAppraisalController(PerformanceAppraisalService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping("/cycles")
    public ResponseEntity<List<PerformanceAppraisalApi.PerformanceCycleResponse>> listCycles() {
        return ResponseEntity.ok(performanceService.listCycles());
    }

    @PostMapping("/cycles")
    public ResponseEntity<PerformanceAppraisalApi.PerformanceCycleResponse> createCycle(
            @Valid @RequestBody PerformanceAppraisalApi.CreateCycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createCycle(request));
    }

    @PostMapping("/cycles/{id}/lock")
    public ResponseEntity<PerformanceAppraisalApi.PerformanceCycleResponse> lockCycle(@PathVariable String id) {
        return ResponseEntity.ok(performanceService.lockCycle(id));
    }

    @GetMapping("/kpis")
    public ResponseEntity<List<PerformanceAppraisalApi.PerformanceKpiResponse>> listKpis(
            @RequestParam(required = false) String cycleId) {
        return ResponseEntity.ok(performanceService.listKpis(cycleId));
    }

    @PostMapping("/kpis")
    public ResponseEntity<PerformanceAppraisalApi.PerformanceKpiResponse> createKpi(
            @Valid @RequestBody PerformanceAppraisalApi.CreateKpiRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.createKpi(request));
    }

    @GetMapping("/appraisals")
    public ResponseEntity<List<PerformanceAppraisalApi.PerformanceAppraisalResponse>> listAppraisals(
            @RequestParam(required = false) String cycleId,
            @RequestParam(required = false) String employeeId) {
        return ResponseEntity.ok(performanceService.listAppraisals(cycleId, employeeId));
    }

    @PostMapping("/appraisals/init")
    public ResponseEntity<PerformanceAppraisalApi.PerformanceAppraisalResponse> initAppraisal(
            @Valid @RequestBody PerformanceAppraisalApi.InitAppraisalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.initAppraisal(request));
    }

    @PostMapping("/appraisals/{id}/submit")
    public ResponseEntity<PerformanceAppraisalApi.PerformanceAppraisalResponse> submitAppraisal(
            @PathVariable String id,
            @Valid @RequestBody PerformanceAppraisalApi.SubmitAppraisalRequest request) {
        return ResponseEntity.ok(performanceService.submitAppraisal(id, request));
    }

    @PostMapping("/appraisals/{id}/finalize")
    public ResponseEntity<PerformanceAppraisalApi.PerformanceAppraisalResponse> finalizeAppraisal(
            @PathVariable String id) {
        return ResponseEntity.ok(performanceService.finalizeAppraisal(id));
    }
}
