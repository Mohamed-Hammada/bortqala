package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.AppointmentService;
import com.bemo.hr.medical.application.DoctorRosterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final DoctorRosterService rosterService;

    public AppointmentController(AppointmentService appointmentService, DoctorRosterService rosterService) {
        this.appointmentService = appointmentService;
        this.rosterService = rosterService;
    }

    @GetMapping("/rosters")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read')")
    public ResponseEntity<List<DoctorRosterDto>> getAllRosters() {
        return ResponseEntity.ok(rosterService.getAllRosters());
    }

    @GetMapping("/doctors/{doctorId}/rosters")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read')")
    public ResponseEntity<List<DoctorRosterDto>> getRostersForDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(rosterService.getRostersForDoctor(doctorId));
    }

    @PostMapping("/rosters")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<DoctorRosterDto> saveRoster(@Valid @RequestBody SaveDoctorRosterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rosterService.saveRoster(request));
    }

    @DeleteMapping("/rosters/{rosterId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<Void> deleteRoster(@PathVariable String rosterId) {
        rosterService.deleteRoster(rosterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read')")
    public ResponseEntity<List<AvailableSlotDto>> getAvailableSlots(@RequestParam String doctorId,
                                                                   @RequestParam String date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, date));
    }

    @GetMapping("/appointments")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.queue.read')")
    public ResponseEntity<List<ClinicAppointmentResponse>> getAppointments(@RequestParam(required = false) String doctorId,
                                                                           @RequestParam String date) {
        return ResponseEntity.ok(appointmentService.getAppointments(doctorId, date));
    }

    @PostMapping("/appointments")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<ClinicAppointmentResponse> bookAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @PostMapping("/appointments/{id}/confirm")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<ClinicAppointmentResponse> confirmAppointment(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id));
    }

    @PostMapping("/appointments/{id}/check-in")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<ClinicAppointmentResponse> checkInAppointment(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.checkInAppointment(id));
    }

    @PostMapping("/appointments/{id}/no-show")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<ClinicAppointmentResponse> markNoShow(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.markNoShow(id));
    }

    @PostMapping("/appointments/{id}/cancel")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<ClinicAppointmentResponse> cancelAppointment(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    @PostMapping("/appointments/reminders/send")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.manage', 'clinic.queue.manage')")
    public ResponseEntity<Integer> sendAppointmentReminders(@RequestParam String date) {
        return ResponseEntity.ok(appointmentService.sendAppointmentReminders(date));
    }

    @GetMapping("/appointments/metrics")
    @PreAuthorize("@auth.hasAnyPermission('clinic.patients.read', 'clinic.commissions.read')")
    public ResponseEntity<AppointmentMetricsResponse> getAppointmentMetrics(@RequestParam String doctorId,
                                                                            @RequestParam String period) {
        return ResponseEntity.ok(appointmentService.getAppointmentMetrics(doctorId, period));
    }
}
