package com.bemo.hr.medical.application;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.domain.ClinicAppointment;
import com.bemo.hr.medical.domain.ClinicVisit;
import com.bemo.hr.medical.domain.DoctorRoster;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.ClinicAppointmentRepository;
import com.bemo.hr.medical.infrastructure.DoctorRosterRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@Transactional
public class AppointmentService {

    private static final ZoneId CAIRO_ZONE = ZoneId.of("Africa/Cairo");

    private final ClinicAppointmentRepository appointmentRepository;
    private final DoctorRosterRepository rosterRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final ClinicQueueService queueService;

    public AppointmentService(ClinicAppointmentRepository appointmentRepository,
                              DoctorRosterRepository rosterRepository,
                              PatientRepository patientRepository,
                              EmployeeRepository employeeRepository,
                              ClinicQueueService queueService) {
        this.appointmentRepository = appointmentRepository;
        this.rosterRepository = rosterRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.queueService = queueService;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableSlots(String doctorEmployeeId, String visitDate) {
        String appId = TenantContext.require();
        LocalDate date = LocalDate.parse(visitDate);
        // Java DayOfWeek: 1=Mon..7=Sun. Convert to 0=Sun, 1=Mon... 6=Sat
        int weekday = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : date.getDayOfWeek().getValue();

        List<DoctorRoster> rosters = rosterRepository.findAllByAppIdAndDoctorEmployeeIdAndWeekdayAndActiveTrue(
                appId, doctorEmployeeId, weekday
        );

        List<ClinicAppointment> existingAppts = appointmentRepository.findAllByAppIdAndDoctorEmployeeIdAndVisitDateOrderByStartsAtAsc(
                appId, doctorEmployeeId, visitDate
        );

        Map<String, ClinicAppointment> activeByTime = new HashMap<>();
        for (ClinicAppointment a : existingAppts) {
            if (a.getStatus() != ClinicAppointment.Status.CANCELLED) {
                activeByTime.put(a.getStartTime(), a);
            }
        }

        List<AvailableSlotDto> slots = new ArrayList<>();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        for (DoctorRoster roster : rosters) {
            LocalTime current = LocalTime.parse(roster.getStartTime(), timeFmt);
            LocalTime end = LocalTime.parse(roster.getEndTime(), timeFmt);
            int step = roster.getSlotMinutes() > 0 ? roster.getSlotMinutes() : 20;

            while (current.plusMinutes(step).compareTo(end) <= 0) {
                String timeStr = current.format(timeFmt);
                long epoch = LocalDateTime.of(date, current).atZone(CAIRO_ZONE).toInstant().toEpochMilli();

                ClinicAppointment booked = activeByTime.get(timeStr);
                slots.add(new AvailableSlotDto(
                        timeStr,
                        epoch,
                        step,
                        booked == null,
                        booked != null ? booked.getId() : null
                ));

                current = current.plusMinutes(step);
            }
        }

        slots.sort(Comparator.comparingLong(AvailableSlotDto::startsAt));
        return slots;
    }

    public ClinicAppointmentResponse bookAppointment(BookAppointmentRequest request) {
        String appId = TenantContext.require();

        Patient patient = patientRepository.findByAppIdAndId(appId, request.patientId())
                .orElseThrow(() -> new NotFoundException("Patient record not found", "PATIENT_NOT_FOUND"));

        LocalDate date = LocalDate.parse(request.visitDate());
        LocalTime time = LocalTime.parse(request.startTime());
        long startsAt = LocalDateTime.of(date, time).atZone(CAIRO_ZONE).toInstant().toEpochMilli();

        if (startsAt < Instant.now().toEpochMilli() - 60_000L) { // 1-minute grace period
            throw new BusinessRuleException("Cannot book an appointment in the past", "APPT_PAST", HttpStatus.BAD_REQUEST);
        }

        Optional<ClinicAppointment> existing = appointmentRepository.findActiveByDoctorAndStartsAt(
                appId, request.doctorEmployeeId(), startsAt
        );
        if (existing.isPresent()) {
            throw new BusinessRuleException("The selected time slot is already booked", "SLOT_TAKEN", HttpStatus.CONFLICT);
        }

        ClinicAppointment.Source source = ClinicAppointment.Source.PHONE;
        if (request.source() != null && !request.source().trim().isEmpty()) {
            try {
                source = ClinicAppointment.Source.valueOf(request.source().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                source = ClinicAppointment.Source.PHONE;
            }
        }

        ClinicAppointment appointment = new ClinicAppointment(
                request.patientId(),
                request.doctorEmployeeId(),
                request.visitDate(),
                request.startTime(),
                startsAt,
                request.durationMinutes() > 0 ? request.durationMinutes() : 20,
                source,
                request.reason()
        );

        ClinicAppointment saved = appointmentRepository.save(appointment);
        log.info("Booked appointment for patient {} with doctor {} at {} on {} in tenant {}",
                request.patientId(), request.doctorEmployeeId(), request.startTime(), request.visitDate(), appId);

        return toResponse(saved, patient);
    }

    public ClinicAppointmentResponse confirmAppointment(String appointmentId) {
        String appId = TenantContext.require();
        ClinicAppointment appointment = appointmentRepository.findByAppIdAndId(appId, appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found", "APPT_NOT_FOUND"));

        appointment.confirm();
        ClinicAppointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public ClinicAppointmentResponse checkInAppointment(String appointmentId) {
        String appId = TenantContext.require();
        ClinicAppointment appointment = appointmentRepository.findByAppIdAndId(appId, appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found", "APPT_NOT_FOUND"));

        if (appointment.getStatus() == ClinicAppointment.Status.CHECKED_IN) {
            throw new BusinessRuleException("Appointment is already checked in", "APPT_ALREADY_CHECKED_IN", HttpStatus.CONFLICT);
        }

        // Auto-queue visit in doctor queue
        QueueVisitRequest queueReq = new QueueVisitRequest(
                appointment.getPatientId(),
                appointment.getDoctorEmployeeId(),
                appointment.getVisitDate(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "CASH"
        );
        ClinicVisitResponse visit = queueService.queueVisit(queueReq);

        appointment.checkIn(visit.id());
        ClinicAppointment saved = appointmentRepository.save(appointment);
        log.info("Checked in appointment {} -> created clinic visit {} in tenant {}", appointmentId, visit.id(), appId);

        return toResponse(saved);
    }

    public ClinicAppointmentResponse markNoShow(String appointmentId) {
        String appId = TenantContext.require();
        ClinicAppointment appointment = appointmentRepository.findByAppIdAndId(appId, appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found", "APPT_NOT_FOUND"));

        appointment.markNoShow();
        ClinicAppointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public ClinicAppointmentResponse cancelAppointment(String appointmentId) {
        String appId = TenantContext.require();
        ClinicAppointment appointment = appointmentRepository.findByAppIdAndId(appId, appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found", "APPT_NOT_FOUND"));

        appointment.cancel();
        ClinicAppointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicAppointmentResponse> getAppointments(String doctorEmployeeId, String visitDate) {
        String appId = TenantContext.require();
        List<ClinicAppointment> list;
        if (doctorEmployeeId != null && !doctorEmployeeId.trim().isEmpty()) {
            list = appointmentRepository.findAllByAppIdAndDoctorEmployeeIdAndVisitDateOrderByStartsAtAsc(appId, doctorEmployeeId.trim(), visitDate);
        } else {
            list = appointmentRepository.findAllByAppIdAndVisitDateOrderByStartsAtAsc(appId, visitDate);
        }
        return list.stream().map(this::toResponse).toList();
    }

    public int sendAppointmentReminders(String targetDate) {
        String appId = TenantContext.require();
        List<ClinicAppointment> pending = appointmentRepository.findPendingRemindersForDate(appId, targetDate);

        for (ClinicAppointment a : pending) {
            a.markReminderSent();
            appointmentRepository.save(a);
        }

        log.info("Dispatched and logged {} appointment reminders for date {} in tenant {}", pending.size(), targetDate, appId);
        return pending.size();
    }

    @Transactional(readOnly = true)
    public AppointmentMetricsResponse getAppointmentMetrics(String doctorEmployeeId, String period) {
        String appId = TenantContext.require();
        List<ClinicAppointment> all = appointmentRepository.findAllForDoctorInPeriod(appId, doctorEmployeeId, period);

        int total = all.size();
        int booked = 0;
        int confirmed = 0;
        int checkedIn = 0;
        int completed = 0;
        int noShow = 0;
        int cancelled = 0;

        for (ClinicAppointment a : all) {
            switch (a.getStatus()) {
                case BOOKED -> booked++;
                case CONFIRMED -> confirmed++;
                case CHECKED_IN -> checkedIn++;
                case DONE -> completed++;
                case NO_SHOW -> noShow++;
                case CANCELLED -> cancelled++;
            }
        }

        BigDecimal rate = BigDecimal.ZERO;
        if (total > 0) {
            rate = BigDecimal.valueOf(noShow * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
        }

        return new AppointmentMetricsResponse(
                period,
                total,
                booked,
                confirmed,
                checkedIn,
                completed,
                noShow,
                cancelled,
                rate
        );
    }

    private ClinicAppointmentResponse toResponse(ClinicAppointment a) {
        String appId = TenantContext.require();
        Patient patient = patientRepository.findByAppIdAndId(appId, a.getPatientId()).orElse(null);
        return toResponse(a, patient);
    }

    private ClinicAppointmentResponse toResponse(ClinicAppointment a, Patient patient) {
        String doctorName = employeeRepository.findById(a.getDoctorEmployeeId())
                .map(com.bemo.hr.employee.domain.Employee::getFullName)
                .orElse(a.getDoctorEmployeeId());

        return new ClinicAppointmentResponse(
                a.getId(),
                a.getPatientId(),
                patient != null ? patient.getFullName() : "Unknown",
                patient != null ? patient.getMrn() : "",
                patient != null ? patient.getPhone() : "",
                a.getDoctorEmployeeId(),
                doctorName,
                a.getVisitDate(),
                a.getStartTime(),
                a.getStartsAt(),
                a.getDurationMinutes(),
                a.getStatus().name(),
                a.getSource().name(),
                a.getReason(),
                a.getClinicVisitId(),
                a.getReminderSentAt(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
