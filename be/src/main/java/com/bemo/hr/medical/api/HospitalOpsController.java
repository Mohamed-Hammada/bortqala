package com.bemo.hr.medical.api;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.HospitalOpsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clinic/hospital")
@RequiredArgsConstructor
public class HospitalOpsController {

    private final HospitalOpsService hospitalOpsService;

    // --- Wards & Rooms ---

    @GetMapping("/wards")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalWardDto>> getWards() {
        return ResponseEntity.ok(hospitalOpsService.getWards());
    }

    @PostMapping("/wards")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalWardDto> saveWard(@Valid @RequestBody SaveHospitalWardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.saveWard(request));
    }

    @GetMapping("/rooms")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalRoomDto>> getRooms(@RequestParam(required = false) String wardId) {
        return ResponseEntity.ok(hospitalOpsService.getRooms(wardId));
    }

    @PostMapping("/rooms")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalRoomDto> saveRoom(@Valid @RequestBody SaveHospitalRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.saveRoom(request));
    }

    // --- Beds & Live Board ---

    @GetMapping("/beds")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalBedDto>> getBeds() {
        return ResponseEntity.ok(hospitalOpsService.getBeds());
    }

    @PostMapping("/beds")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalBedDto> saveBed(@Valid @RequestBody SaveHospitalBedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.saveBed(request));
    }

    // --- ADT: Admissions, Transfers, Discharges ---

    @PostMapping("/admissions/admit")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalAdmissionDto> admitPatient(@Valid @RequestBody AdmitPatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.admitPatient(request));
    }

    @PostMapping("/admissions/{id}/transfer")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalAdmissionDto> transferPatient(
            @PathVariable String id,
            @Valid @RequestBody TransferPatientBedRequest request
    ) {
        return ResponseEntity.ok(hospitalOpsService.transferPatient(id, request));
    }

    @PostMapping("/admissions/{id}/discharge")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalAdmissionDto> dischargePatient(
            @PathVariable String id,
            @Valid @RequestBody DischargePatientRequest request
    ) {
        return ResponseEntity.ok(hospitalOpsService.dischargePatient(id, request));
    }

    @GetMapping("/admissions")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalAdmissionDto>> getAdmissions(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(hospitalOpsService.getAdmissions(status));
    }

    @GetMapping("/metrics/occupancy")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<HospitalOccupancyMetricsDto> getOccupancyMetrics() {
        return ResponseEntity.ok(hospitalOpsService.getOccupancyMetrics());
    }

    // --- Operating Theater (OT) ---

    @GetMapping("/ot/schedules")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalOtScheduleDto>> getOtSchedules() {
        return ResponseEntity.ok(hospitalOpsService.getOtSchedules());
    }

    @PostMapping("/ot/schedules")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalOtScheduleDto> scheduleOt(@Valid @RequestBody ScheduleOtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.scheduleOt(request));
    }

    @PostMapping("/ot/schedules/{id}/start")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalOtScheduleDto> startOt(@PathVariable String id) {
        return ResponseEntity.ok(hospitalOpsService.startOt(id));
    }

    @PostMapping("/ot/schedules/{id}/complete")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalOtScheduleDto> completeOt(
            @PathVariable String id,
            @Valid @RequestBody CompleteOtSurgeryRequest request
    ) {
        return ResponseEntity.ok(hospitalOpsService.completeOt(id, request));
    }

    @PostMapping("/ot/schedules/{id}/charges")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalOtChargeDto> addOtCharge(
            @PathVariable String id,
            @Valid @RequestBody AddOtChargeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.addOtCharge(id, request));
    }

    // --- Nursing: MAR, Fluid I/O, Notes ---

    @GetMapping("/nursing/mar/admission/{admissionId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalMarEntryDto>> getMarEntries(@PathVariable String admissionId) {
        return ResponseEntity.ok(hospitalOpsService.getMarEntries(admissionId));
    }

    @PostMapping("/nursing/mar")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalMarEntryDto> createMarEntry(@Valid @RequestBody CreateMarEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.createMarEntry(request));
    }

    @PostMapping("/nursing/mar/{id}/administer")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalMarEntryDto> administerMarEntry(
            @PathVariable String id,
            @Valid @RequestBody AdministerMarEntryRequest request
    ) {
        return ResponseEntity.ok(hospitalOpsService.administerMarEntry(id, request));
    }

    @GetMapping("/nursing/fluid-io/admission/{admissionId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalFluidIoEntryDto>> getFluidIoEntries(@PathVariable String admissionId) {
        return ResponseEntity.ok(hospitalOpsService.getFluidIoEntries(admissionId));
    }

    @PostMapping("/nursing/fluid-io")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalFluidIoEntryDto> recordFluidIo(@Valid @RequestBody RecordFluidIoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.recordFluidIo(request));
    }

    @GetMapping("/nursing/notes/admission/{admissionId}")
    @PreAuthorize("@auth.hasAnyPermission('clinic.hospital.read', 'clinic.hospital.manage')")
    public ResponseEntity<List<HospitalNursingNoteDto>> getNursingNotes(@PathVariable String admissionId) {
        return ResponseEntity.ok(hospitalOpsService.getNursingNotes(admissionId));
    }

    @PostMapping("/nursing/notes")
    @PreAuthorize("@auth.hasPermission('clinic.hospital.manage')")
    public ResponseEntity<HospitalNursingNoteDto> addNursingNote(@Valid @RequestBody AddNursingNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalOpsService.addNursingNote(request));
    }
}
