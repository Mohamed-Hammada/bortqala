package com.bemo.hr.medical.application;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HospitalOpsService {

    private final HospitalWardRepository wardRepository;
    private final HospitalRoomRepository roomRepository;
    private final HospitalBedRepository bedRepository;
    private final HospitalAdmissionRepository admissionRepository;
    private final HospitalBedStayRepository bedStayRepository;
    private final HospitalOtScheduleRepository otScheduleRepository;
    private final HospitalOtChargeRepository otChargeRepository;
    private final HospitalMarEntryRepository marEntryRepository;
    private final HospitalFluidIoEntryRepository fluidIoEntryRepository;
    private final HospitalNursingNoteRepository nursingNoteRepository;
    private final PatientRepository patientRepository;
    private final DoctorRosterRepository doctorRosterRepository;

    private String getAppId() {
        return TenantContext.require();
    }

    // --- Wards & Rooms ---

    @Transactional(readOnly = true)
    public List<HospitalWardDto> getWards() {
        String appId = getAppId();
        return wardRepository.findAllByAppIdOrderByNameAsc(appId).stream()
                .map(this::mapWard)
                .collect(Collectors.toList());
    }

    public HospitalWardDto saveWard(SaveHospitalWardRequest request) {
        String appId = getAppId();
        HospitalWard ward = new HospitalWard(request.code(), request.name(), request.departmentId());
        ward.setAppId(appId);
        ward.setActive(request.active());
        return mapWard(wardRepository.save(ward));
    }

    @Transactional(readOnly = true)
    public List<HospitalRoomDto> getRooms(String wardId) {
        String appId = getAppId();
        List<HospitalRoom> rooms = wardId != null && !wardId.isBlank()
                ? roomRepository.findAllByAppIdAndWardIdOrderByRoomNumberAsc(appId, wardId)
                : roomRepository.findAllByAppIdOrderByRoomNumberAsc(appId);
        return rooms.stream().map(this::mapRoom).collect(Collectors.toList());
    }

    public HospitalRoomDto saveRoom(SaveHospitalRoomRequest request) {
        String appId = getAppId();
        HospitalRoom.Type roomType = HospitalRoom.Type.STANDARD;
        if (request.roomType() != null && !request.roomType().isBlank()) {
            try {
                roomType = HospitalRoom.Type.valueOf(request.roomType());
            } catch (IllegalArgumentException ignored) {}
        }
        HospitalRoom room = new HospitalRoom(request.wardId(), request.roomNumber(), roomType);
        room.setAppId(appId);
        room.setActive(request.active());
        return mapRoom(roomRepository.save(room));
    }

    // --- Beds & Live Board ---

    @Transactional(readOnly = true)
    public List<HospitalBedDto> getBeds() {
        String appId = getAppId();
        List<HospitalBed> beds = bedRepository.findAllByAppIdOrderByBedNumberAsc(appId);
        Map<String, HospitalRoom> roomMap = roomRepository.findAllByAppIdOrderByRoomNumberAsc(appId).stream()
                .collect(Collectors.toMap(HospitalRoom::getId, r -> r, (a, b) -> a));
        Map<String, HospitalWard> wardMap = wardRepository.findAllByAppIdOrderByNameAsc(appId).stream()
                .collect(Collectors.toMap(HospitalWard::getId, w -> w, (a, b) -> a));
        Map<String, Patient> patientMap = patientRepository.findAllByAppId(appId).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a));
        Map<String, HospitalAdmission> admissionMap = admissionRepository.findAllByAppIdAndStatusOrderByAdmittedAtDesc(appId, HospitalAdmission.Status.ADMITTED).stream()
                .collect(Collectors.toMap(HospitalAdmission::getId, a -> a, (a, b) -> a));

        return beds.stream()
                .map(bed -> mapBed(bed, roomMap, wardMap, admissionMap, patientMap))
                .collect(Collectors.toList());
    }

    public HospitalBedDto saveBed(SaveHospitalBedRequest request) {
        String appId = getAppId();
        HospitalBed.Status status = HospitalBed.Status.FREE;
        if (request.status() != null && !request.status().isBlank()) {
            try {
                status = HospitalBed.Status.valueOf(request.status());
            } catch (IllegalArgumentException ignored) {}
        }
        HospitalBed bed = new HospitalBed(request.roomId(), request.bedNumber(), status);
        bed.setAppId(appId);
        bed.setActive(request.active());
        HospitalBed saved = bedRepository.save(bed);
        return mapBed(saved, Map.of(), Map.of(), Map.of(), Map.of());
    }

    // --- ADT: Admit / Transfer / Discharge ---

    public HospitalAdmissionDto admitPatient(AdmitPatientRequest request) {
        String appId = getAppId();
        Patient patient = patientRepository.findByAppIdAndId(appId, request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient not found", "PATIENT_NOT_FOUND"));

        // Check if patient already has an active open admission
        Optional<HospitalAdmission> existing = admissionRepository.findByAppIdAndPatientIdAndStatus(
                appId, request.patientId(), HospitalAdmission.Status.ADMITTED);
        if (existing.isPresent()) {
            throw new BusinessRuleException("Patient already has an active admission", "ADMISSION_ALREADY_OPEN", HttpStatus.BAD_REQUEST);
        }

        // Check bed availability
        HospitalBed bed = bedRepository.findByAppIdAndId(appId, request.bedId())
                .orElseThrow(() -> new NotFoundException("Bed not found", "BED_NOT_FOUND"));
        if (bed.getStatus() != HospitalBed.Status.FREE) {
            throw new BusinessRuleException("Hospital bed is occupied", "BED_NOT_FREE", HttpStatus.BAD_REQUEST);
        }

        HospitalAdmission admission = new HospitalAdmission(
                patient.getId(),
                request.admittingDoctorId(),
                bed.getId(),
                request.chiefComplaint()
        );
        admission.setAppId(appId);
        HospitalAdmission savedAdmission = admissionRepository.save(admission);

        // Occupy bed
        bed.occupy(savedAdmission.getId());
        bedRepository.save(bed);

        // Create initial bed stay
        HospitalBedStay stay = new HospitalBedStay(savedAdmission.getId(), bed.getId(), "Initial Admission");
        stay.setAppId(appId);
        bedStayRepository.save(stay);

        return mapAdmission(savedAdmission, List.of(stay), patient);
    }

    public HospitalAdmissionDto transferPatient(String admissionId, TransferPatientBedRequest request) {
        String appId = getAppId();
        HospitalAdmission admission = admissionRepository.findByAppIdAndId(appId, admissionId)
                .orElseThrow(() -> new NotFoundException("Admission not found", "ADMISSION_NOT_FOUND"));

        if (admission.getStatus() == HospitalAdmission.Status.DISCHARGED) {
            throw new BusinessRuleException("Admission already discharged", "ADMISSION_ALREADY_DISCHARGED", HttpStatus.BAD_REQUEST);
        }

        HospitalBed targetBed = bedRepository.findByAppIdAndId(appId, request.targetBedId())
                .orElseThrow(() -> new NotFoundException("Target bed not found", "BED_NOT_FOUND"));
        if (targetBed.getStatus() != HospitalBed.Status.FREE) {
            throw new BusinessRuleException("Target bed is not free", "BED_NOT_FREE", HttpStatus.BAD_REQUEST);
        }

        // Free old bed
        if (admission.getCurrentBedId() != null) {
            bedRepository.findByAppIdAndId(appId, admission.getCurrentBedId()).ifPresent(oldBed -> {
                oldBed.free();
                bedRepository.save(oldBed);
            });
        }

        // Close previous active stay
        bedStayRepository.findByAppIdAndAdmissionIdAndEndedAtIsNull(appId, admission.getId())
                .ifPresent(stay -> {
                    stay.endStay();
                    bedStayRepository.save(stay);
                });

        // Occupy target bed
        targetBed.occupy(admission.getId());
        bedRepository.save(targetBed);

        // Record new stay
        HospitalBedStay newStay = new HospitalBedStay(admission.getId(), targetBed.getId(), request.transferReason());
        newStay.setAppId(appId);
        bedStayRepository.save(newStay);

        admission.transferBed(targetBed.getId());
        HospitalAdmission saved = admissionRepository.save(admission);

        List<HospitalBedStay> allStays = bedStayRepository.findAllByAppIdAndAdmissionIdOrderByStartedAtAsc(appId, saved.getId());
        Patient patient = patientRepository.findByAppIdAndId(appId, saved.getPatientId()).orElse(null);
        return mapAdmission(saved, allStays, patient);
    }

    public HospitalAdmissionDto dischargePatient(String admissionId, DischargePatientRequest request) {
        String appId = getAppId();
        HospitalAdmission admission = admissionRepository.findByAppIdAndId(appId, admissionId)
                .orElseThrow(() -> new NotFoundException("Admission not found", "ADMISSION_NOT_FOUND"));

        if (admission.getStatus() == HospitalAdmission.Status.DISCHARGED) {
            throw new BusinessRuleException("Admission already discharged", "ADMISSION_ALREADY_DISCHARGED", HttpStatus.BAD_REQUEST);
        }

        if (request.dischargeSummary() == null || request.dischargeSummary().trim().length() < 20) {
            throw new BusinessRuleException("Discharge summary required (min 20 chars)", "DISCHARGE_SUMMARY_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        // Free bed
        if (admission.getCurrentBedId() != null) {
            bedRepository.findByAppIdAndId(appId, admission.getCurrentBedId()).ifPresent(bed -> {
                bed.free();
                bedRepository.save(bed);
            });
        }

        // Close stay
        bedStayRepository.findByAppIdAndAdmissionIdAndEndedAtIsNull(appId, admission.getId())
                .ifPresent(stay -> {
                    stay.endStay();
                    bedStayRepository.save(stay);
                });

        admission.discharge(request.dischargeSummary().trim());
        HospitalAdmission saved = admissionRepository.save(admission);

        List<HospitalBedStay> allStays = bedStayRepository.findAllByAppIdAndAdmissionIdOrderByStartedAtAsc(appId, saved.getId());
        Patient patient = patientRepository.findByAppIdAndId(appId, saved.getPatientId()).orElse(null);
        return mapAdmission(saved, allStays, patient);
    }

    @Transactional(readOnly = true)
    public List<HospitalAdmissionDto> getAdmissions(String status) {
        String appId = getAppId();
        List<HospitalAdmission> admissions = status != null && !status.isBlank()
                ? admissionRepository.findAllByAppIdAndStatusOrderByAdmittedAtDesc(appId, HospitalAdmission.Status.valueOf(status))
                : admissionRepository.findAllByAppIdOrderByAdmittedAtDesc(appId);

        Map<String, Patient> patientMap = patientRepository.findAllByAppId(appId).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a));

        return admissions.stream()
                .map(a -> {
                    List<HospitalBedStay> stays = bedStayRepository.findAllByAppIdAndAdmissionIdOrderByStartedAtAsc(appId, a.getId());
                    return mapAdmission(a, stays, patientMap.get(a.getPatientId()));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HospitalOccupancyMetricsDto getOccupancyMetrics() {
        String appId = getAppId();
        long totalBeds = bedRepository.countTotalActiveBeds(appId);
        long occupiedBeds = bedRepository.countOccupiedBeds(appId);
        double occupancyRate = totalBeds > 0 ? ((double) occupiedBeds * 100.0 / totalBeds) : 0.0;

        List<HospitalAdmission> discharged = admissionRepository.findAllByAppIdAndStatusOrderByAdmittedAtDesc(appId, HospitalAdmission.Status.DISCHARGED);
        double alos = 0.0;
        if (!discharged.isEmpty()) {
            double totalStayDays = discharged.stream()
                    .mapToDouble(a -> {
                        long durationMs = (a.getDischargedAt() != null ? a.getDischargedAt() : a.getAdmittedAt()) - a.getAdmittedAt();
                        return Math.max(0.5, (double) durationMs / (1000.0 * 60.0 * 60.0 * 24.0));
                    })
                    .sum();
            alos = Math.round((totalStayDays / discharged.size()) * 10.0) / 10.0;
        }

        return new HospitalOccupancyMetricsDto(
                totalBeds,
                occupiedBeds,
                Math.round(occupancyRate * 10.0) / 10.0,
                alos
        );
    }

    // --- Operating Theater (OT) ---

    @Transactional(readOnly = true)
    public List<HospitalOtScheduleDto> getOtSchedules() {
        String appId = getAppId();
        List<HospitalOtSchedule> schedules = otScheduleRepository.findAllByAppIdOrderByPlannedStartDesc(appId);
        Map<String, Patient> patientMap = patientRepository.findAllByAppId(appId).stream()
                .collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a));

        return schedules.stream()
                .map(s -> {
                    List<HospitalOtCharge> charges = otChargeRepository.findAllByAppIdAndOtScheduleIdOrderByChargedAtAsc(appId, s.getId());
                    return mapOtSchedule(s, charges, patientMap.get(s.getPatientId()));
                })
                .collect(Collectors.toList());
    }

    public HospitalOtScheduleDto scheduleOt(ScheduleOtRequest request) {
        String appId = getAppId();
        HospitalOtSchedule schedule = new HospitalOtSchedule(
                request.theaterName(),
                request.patientId(),
                request.surgeonDoctorId(),
                request.surgeryType(),
                request.plannedStart(),
                request.durationMinutes()
        );
        schedule.setAppId(appId);
        HospitalOtSchedule saved = otScheduleRepository.save(schedule);
        Patient patient = patientRepository.findByAppIdAndId(appId, request.patientId()).orElse(null);
        return mapOtSchedule(saved, List.of(), patient);
    }

    public HospitalOtScheduleDto startOt(String id) {
        String appId = getAppId();
        HospitalOtSchedule schedule = otScheduleRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("OT schedule not found", "OT_SCHEDULE_NOT_FOUND"));
        schedule.startSurgery();
        HospitalOtSchedule saved = otScheduleRepository.save(schedule);
        List<HospitalOtCharge> charges = otChargeRepository.findAllByAppIdAndOtScheduleIdOrderByChargedAtAsc(appId, saved.getId());
        Patient patient = patientRepository.findByAppIdAndId(appId, saved.getPatientId()).orElse(null);
        return mapOtSchedule(saved, charges, patient);
    }

    public HospitalOtScheduleDto completeOt(String id, CompleteOtSurgeryRequest request) {
        String appId = getAppId();
        HospitalOtSchedule schedule = otScheduleRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("OT schedule not found", "OT_SCHEDULE_NOT_FOUND"));
        schedule.completeSurgery(request.anesthesiaNotes(), request.surgicalNotes());
        HospitalOtSchedule saved = otScheduleRepository.save(schedule);
        List<HospitalOtCharge> charges = otChargeRepository.findAllByAppIdAndOtScheduleIdOrderByChargedAtAsc(appId, saved.getId());
        Patient patient = patientRepository.findByAppIdAndId(appId, saved.getPatientId()).orElse(null);
        return mapOtSchedule(saved, charges, patient);
    }

    public HospitalOtChargeDto addOtCharge(String otScheduleId, AddOtChargeRequest request) {
        String appId = getAppId();
        otScheduleRepository.findByAppIdAndId(appId, otScheduleId)
                .orElseThrow(() -> new NotFoundException("OT schedule not found", "OT_SCHEDULE_NOT_FOUND"));

        HospitalOtCharge charge = new HospitalOtCharge(otScheduleId, request.itemName(), request.quantity(), request.unitPrice());
        charge.setAppId(appId);
        HospitalOtCharge saved = otChargeRepository.save(charge);
        return mapOtCharge(saved);
    }

    // --- Nursing: MAR, Fluid I/O, Notes ---

    @Transactional(readOnly = true)
    public List<HospitalMarEntryDto> getMarEntries(String admissionId) {
        String appId = getAppId();
        return marEntryRepository.findAllByAppIdAndAdmissionIdOrderByDueAtAsc(appId, admissionId).stream()
                .map(this::mapMar)
                .collect(Collectors.toList());
    }

    public HospitalMarEntryDto createMarEntry(CreateMarEntryRequest request) {
        String appId = getAppId();
        admissionRepository.findByAppIdAndId(appId, request.admissionId())
                .orElseThrow(() -> new NotFoundException("Admission not found", "ADMISSION_NOT_FOUND"));

        HospitalMarEntry entry = new HospitalMarEntry(
                request.admissionId(),
                request.medicationName(),
                request.dose(),
                request.route(),
                request.dueAt()
        );
        entry.setAppId(appId);
        return mapMar(marEntryRepository.save(entry));
    }

    public HospitalMarEntryDto administerMarEntry(String id, AdministerMarEntryRequest request) {
        String appId = getAppId();
        HospitalMarEntry entry = marEntryRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new NotFoundException("MAR entry not found", "MAR_ENTRY_NOT_FOUND"));

        if (entry.getStatus() == HospitalMarEntry.Status.GIVEN) {
            throw new BusinessRuleException("MAR entry already administered", "MAR_INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST);
        }

        HospitalMarEntry.Status targetStatus = HospitalMarEntry.Status.GIVEN;
        try {
            targetStatus = HospitalMarEntry.Status.valueOf(request.status());
        } catch (IllegalArgumentException ignored) {}

        entry.administer(targetStatus, request.nurseId(), request.nurseName(), request.notes());
        return mapMar(marEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<HospitalFluidIoEntryDto> getFluidIoEntries(String admissionId) {
        String appId = getAppId();
        return fluidIoEntryRepository.findAllByAppIdAndAdmissionIdOrderByEntryTimeAsc(appId, admissionId).stream()
                .map(this::mapFluidIo)
                .collect(Collectors.toList());
    }

    public HospitalFluidIoEntryDto recordFluidIo(RecordFluidIoRequest request) {
        String appId = getAppId();
        admissionRepository.findByAppIdAndId(appId, request.admissionId())
                .orElseThrow(() -> new NotFoundException("Admission not found", "ADMISSION_NOT_FOUND"));

        HospitalFluidIoEntry.Type type = HospitalFluidIoEntry.Type.INTAKE;
        try {
            type = HospitalFluidIoEntry.Type.valueOf(request.type());
        } catch (IllegalArgumentException ignored) {}

        HospitalFluidIoEntry entry = new HospitalFluidIoEntry(
                request.admissionId(),
                type,
                request.routeOrFluid(),
                request.amountMl(),
                request.recordedBy()
        );
        entry.setAppId(appId);
        return mapFluidIo(fluidIoEntryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<HospitalNursingNoteDto> getNursingNotes(String admissionId) {
        String appId = getAppId();
        return nursingNoteRepository.findAllByAppIdAndAdmissionIdOrderByRecordedAtDesc(appId, admissionId).stream()
                .map(this::mapNursingNote)
                .collect(Collectors.toList());
    }

    public HospitalNursingNoteDto addNursingNote(AddNursingNoteRequest request) {
        String appId = getAppId();
        admissionRepository.findByAppIdAndId(appId, request.admissionId())
                .orElseThrow(() -> new NotFoundException("Admission not found", "ADMISSION_NOT_FOUND"));

        HospitalNursingNote note = new HospitalNursingNote(
                request.admissionId(),
                request.nurseName(),
                request.noteText()
        );
        note.setAppId(appId);
        return mapNursingNote(nursingNoteRepository.save(note));
    }

    // --- Mappers ---

    private HospitalWardDto mapWard(HospitalWard w) {
        return new HospitalWardDto(w.getId(), w.getCode(), w.getName(), w.getDepartmentId(), w.isActive(), w.getCreatedAt(), w.getUpdatedAt());
    }

    private HospitalRoomDto mapRoom(HospitalRoom r) {
        return new HospitalRoomDto(r.getId(), r.getWardId(), r.getRoomNumber(), r.getRoomType().name(), r.isActive(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private HospitalBedDto mapBed(HospitalBed b, Map<String, HospitalRoom> roomMap, Map<String, HospitalWard> wardMap,
                                  Map<String, HospitalAdmission> admissionMap, Map<String, Patient> patientMap) {
        HospitalRoom room = roomMap.get(b.getRoomId());
        HospitalWard ward = room != null ? wardMap.get(room.getWardId()) : null;
        HospitalAdmission admission = b.getCurrentAdmissionId() != null ? admissionMap.get(b.getCurrentAdmissionId()) : null;
        Patient patient = admission != null ? patientMap.get(admission.getPatientId()) : null;

        return new HospitalBedDto(
                b.getId(),
                b.getRoomId(),
                room != null ? room.getRoomNumber() : null,
                ward != null ? ward.getId() : null,
                ward != null ? ward.getName() : null,
                b.getBedNumber(),
                b.getStatus().name(),
                b.getCurrentAdmissionId(),
                patient != null ? patient.getFullName() : null,
                patient != null ? patient.getMrn() : null,
                b.isActive(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

    private HospitalAdmissionDto mapAdmission(HospitalAdmission a, List<HospitalBedStay> stays, Patient patient) {
        List<HospitalBedStayDto> stayDtos = stays.stream()
                .map(s -> new HospitalBedStayDto(
                        s.getId(),
                        s.getAdmissionId(),
                        s.getBedId(),
                        null,
                        null,
                        null,
                        s.getStartedAt(),
                        s.getEndedAt(),
                        s.getTransferReason()
                ))
                .collect(Collectors.toList());

        return new HospitalAdmissionDto(
                a.getId(),
                a.getPatientId(),
                patient != null ? patient.getMrn() : null,
                patient != null ? patient.getFullName() : null,
                a.getAdmittingDoctorId(),
                null,
                a.getCurrentBedId(),
                null,
                null,
                null,
                a.getStatus().name(),
                a.getChiefComplaint(),
                a.getAdmittedAt(),
                a.getDischargedAt(),
                a.getDischargeSummary(),
                stayDtos,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private HospitalOtScheduleDto mapOtSchedule(HospitalOtSchedule s, List<HospitalOtCharge> charges, Patient patient) {
        List<HospitalOtChargeDto> chargeDtos = charges.stream().map(this::mapOtCharge).collect(Collectors.toList());
        return new HospitalOtScheduleDto(
                s.getId(),
                s.getTheaterName(),
                s.getPatientId(),
                patient != null ? patient.getMrn() : null,
                patient != null ? patient.getFullName() : null,
                s.getSurgeonDoctorId(),
                null,
                s.getSurgeryType(),
                s.getStatus().name(),
                s.getPlannedStart(),
                s.getDurationMinutes(),
                s.getActualStart(),
                s.getActualEnd(),
                s.getAnesthesiaNotes(),
                s.getSurgicalNotes(),
                chargeDtos,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private HospitalOtChargeDto mapOtCharge(HospitalOtCharge c) {
        return new HospitalOtChargeDto(
                c.getId(),
                c.getOtScheduleId(),
                c.getItemName(),
                c.getQuantity(),
                c.getUnitPrice(),
                c.getTotalAmount(),
                c.getChargedAt()
        );
    }

    private HospitalMarEntryDto mapMar(HospitalMarEntry m) {
        return new HospitalMarEntryDto(
                m.getId(),
                m.getAdmissionId(),
                m.getMedicationName(),
                m.getDose(),
                m.getRoute(),
                m.getDueAt(),
                m.getStatus().name(),
                m.getAdministeredAt(),
                m.getNurseId(),
                m.getNurseName(),
                m.getNotes(),
                m.getCreatedAt()
        );
    }

    private HospitalFluidIoEntryDto mapFluidIo(HospitalFluidIoEntry f) {
        return new HospitalFluidIoEntryDto(
                f.getId(),
                f.getAdmissionId(),
                f.getEntryTime(),
                f.getType().name(),
                f.getRouteOrFluid(),
                f.getAmountMl(),
                f.getRecordedBy()
        );
    }

    private HospitalNursingNoteDto mapNursingNote(HospitalNursingNote n) {
        return new HospitalNursingNoteDto(
                n.getId(),
                n.getAdmissionId(),
                n.getRecordedAt(),
                n.getNurseName(),
                n.getNoteText()
        );
    }
}
