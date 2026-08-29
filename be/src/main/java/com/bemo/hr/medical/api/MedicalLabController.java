package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.MedicalLabService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic/lab")
public class MedicalLabController {

    private final MedicalLabService labService;

    public MedicalLabController(MedicalLabService labService) {
        this.labService = labService;
    }

    @GetMapping("/tests")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read', 'clinic.lab.read')")
    public ResponseEntity<List<LabTestItemDto>> getAllLabTests(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(labService.getAllLabTests(category));
    }

    @PostMapping("/tests")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabTestItemDto> saveLabTest(@Valid @RequestBody SaveLabTestItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labService.saveLabTest(request));
    }

    @GetMapping("/orders")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.lab.read')")
    public ResponseEntity<List<LabOrderDto>> getAllOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(labService.getAllOrders(status));
    }

    @PostMapping("/orders")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> createLabOrder(@Valid @RequestBody CreateLabOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labService.createLabOrder(request));
    }

    @PostMapping("/orders/{id}/collect")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> collectSample(@PathVariable String id) {
        return ResponseEntity.ok(labService.collectSample(id));
    }

    @PostMapping("/orders/{id}/send-out")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> sendOutOrder(@PathVariable String id, @RequestBody SendOutLabOrderRequest request) {
        return ResponseEntity.ok(labService.sendOutOrder(id, request));
    }

    @PostMapping("/orders/{id}/result")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> enterResult(@PathVariable String id, @Valid @RequestBody EnterLabResultRequest request) {
        return ResponseEntity.ok(labService.enterResult(id, request));
    }

    @PostMapping("/orders/{id}/validate")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> validateOrder(@PathVariable String id) {
        return ResponseEntity.ok(labService.validateOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok(labService.cancelOrder(id));
    }

    @PostMapping("/orders/{id}/ack-critical")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.lab.manage')")
    public ResponseEntity<LabOrderDto> acknowledgeCritical(@PathVariable String id) {
        return ResponseEntity.ok(labService.acknowledgeCritical(id));
    }

    @GetMapping("/orders/aging")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.lab.read')")
    public ResponseEntity<List<LabOrderDto>> getAgingSentOutOrders() {
        return ResponseEntity.ok(labService.getAgingSentOutOrders());
    }

    @GetMapping("/orders/patient/{patientId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.lab.read')")
    public ResponseEntity<List<LabOrderDto>> getOrdersByPatient(@PathVariable String patientId,
                                                                @RequestParam(defaultValue = "false") boolean validatedOnly) {
        return ResponseEntity.ok(labService.getOrdersByPatient(patientId, validatedOnly));
    }
}
