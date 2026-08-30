package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.ClinicPrescriptionService;
import com.bemo.hr.medical.application.ClinicQueueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic")
@RequiredArgsConstructor
public class ClinicQueueController {

    private final ClinicQueueService queueService;
    private final ClinicPrescriptionService prescriptionService;

    @GetMapping("/queue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClinicVisitResponse>> getQueue(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String doctorId
    ) {
        return ResponseEntity.ok(queueService.getQueueForDate(date, doctorId));
    }

    @PostMapping("/queue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClinicVisitResponse> queueVisit(@Valid @RequestBody QueueVisitRequest request) {
        return ResponseEntity.ok(queueService.queueVisit(request));
    }

    @PostMapping("/queue/{id}/call")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClinicVisitResponse> callVisit(@PathVariable String id) {
        return ResponseEntity.ok(queueService.callVisit(id));
    }

    @PostMapping("/queue/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClinicVisitResponse> completeVisit(
            @PathVariable String id,
            @Valid @RequestBody CompleteVisitRequest request
    ) {
        return ResponseEntity.ok(queueService.completeVisit(id, request));
    }

    @PostMapping("/queue/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClinicVisitResponse> cancelVisit(@PathVariable String id) {
        return ResponseEntity.ok(queueService.cancelVisit(id));
    }

    @GetMapping("/visits/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClinicVisitResponse> getVisit(@PathVariable String id) {
        return ResponseEntity.ok(queueService.getVisit(id));
    }

    @PostMapping("/visits/{id}/prescriptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionLineResponse>> savePrescriptions(
            @PathVariable String id,
            @Valid @RequestBody List<PrescriptionLineRequest> lines
    ) {
        return ResponseEntity.ok(prescriptionService.savePrescriptions(id, lines));
    }

    @GetMapping("/visits/{id}/prescriptions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrescriptionLineResponse>> getPrescriptions(@PathVariable String id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptions(id));
    }
}
