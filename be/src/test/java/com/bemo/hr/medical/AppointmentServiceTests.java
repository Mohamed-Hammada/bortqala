package com.bemo.hr.medical;

import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.AppointmentService;
import com.bemo.hr.medical.application.ClinicQueueService;
import com.bemo.hr.medical.domain.ClinicAppointment;
import com.bemo.hr.medical.domain.DoctorRoster;
import com.bemo.hr.medical.domain.Patient;
import com.bemo.hr.medical.infrastructure.ClinicAppointmentRepository;
import com.bemo.hr.medical.infrastructure.DoctorRosterRepository;
import com.bemo.hr.medical.infrastructure.PatientRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTests {

    @Mock
    private ClinicAppointmentRepository appointmentRepository;
    @Mock
    private DoctorRosterRepository rosterRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ClinicQueueService queueService;

    private AppointmentService appointmentService;

    private final String APP_ID = "tenant-medical";
    private final String DOCTOR_ID = "doc-1";
    private final String PATIENT_ID = "pat-1";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        appointmentService = new AppointmentService(
                appointmentRepository,
                rosterRepository,
                patientRepository,
                employeeRepository,
                queueService
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getAvailableSlots_generatesSlotsFromRoster_markingBookedOnes() {
        // Suppose date is a future date, say 2026-09-06 (Sunday -> weekday 0)
        String testDate = "2026-09-06";
        DoctorRoster roster = new DoctorRoster(DOCTOR_ID, 0, "09:00", "10:00", 20, 1, null, null);

        when(rosterRepository.findAllByAppIdAndDoctorEmployeeIdAndWeekdayAndActiveTrue(APP_ID, DOCTOR_ID, 0))
                .thenReturn(List.of(roster));

        // 1 booked appointment at 09:20
        ClinicAppointment appt = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, testDate, "09:20", 1788700000000L, 20, ClinicAppointment.Source.PHONE, "Fever");
        when(appointmentRepository.findAllByAppIdAndDoctorEmployeeIdAndVisitDateOrderByStartsAtAsc(APP_ID, DOCTOR_ID, testDate))
                .thenReturn(List.of(appt));

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(DOCTOR_ID, testDate);

        assertNotNull(slots);
        assertEquals(3, slots.size()); // 09:00, 09:20, 09:40
        assertTrue(slots.get(0).available());
        assertEquals("09:00", slots.get(0).startTime());
        assertFalse(slots.get(1).available()); // 09:20 is booked
        assertEquals("09:20", slots.get(1).startTime());
        assertTrue(slots.get(2).available());
        assertEquals("09:40", slots.get(2).startTime());
    }

    @Test
    void bookAppointment_rejectsDoubleBooking() {
        Patient patient = new Patient("MRN-00001", "29008200101534", "Ahmed Ali", "01001234567", "MALE", "1990-08-20", "O_POSITIVE", null, null, null, null);
        when(patientRepository.findByAppIdAndId(APP_ID, PATIENT_ID)).thenReturn(Optional.of(patient));

        String futureDate = LocalDate.now().plusDays(5).toString();
        ClinicAppointment existingAppt = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, futureDate, "10:00", 1788700000000L, 20, ClinicAppointment.Source.PHONE, "Consult");

        when(appointmentRepository.findActiveByDoctorAndStartsAt(eq(APP_ID), eq(DOCTOR_ID), anyLong()))
                .thenReturn(Optional.of(existingAppt));

        BookAppointmentRequest request = new BookAppointmentRequest(
                PATIENT_ID, DOCTOR_ID, futureDate, "10:00", 20, "PHONE", "Follow up"
        );

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                appointmentService.bookAppointment(request)
        );
        assertEquals("SLOT_TAKEN", ex.getCode());
    }

    @Test
    void checkInAppointment_transitionsToCheckedIn_andCreatesQueueVisit() {
        String testDate = "2026-08-30";
        ClinicAppointment appt = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, testDate, "10:00", 1788700000000L, 20, ClinicAppointment.Source.PHONE, "Consult");
        appt.setId("appt-1");

        when(appointmentRepository.findByAppIdAndId(APP_ID, "appt-1")).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(ClinicAppointment.class))).thenAnswer(inv -> inv.getArgument(0));

        ClinicVisitResponse visitResp = new ClinicVisitResponse(
                "vis-1", PATIENT_ID, "Ahmed Ali", "MRN-00001", "01001234567", DOCTOR_ID, "Dr. Tarek",
                testDate, 1788700000000L, 1, "WAITING", null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "CASH", List.of(), 1000L, 1000L
        );
        when(queueService.queueVisit(any())).thenReturn(visitResp);

        ClinicAppointmentResponse result = appointmentService.checkInAppointment("appt-1");

        assertNotNull(result);
        assertEquals("CHECKED_IN", result.status());
        assertEquals("vis-1", result.clinicVisitId());
        verify(queueService).queueVisit(any());
    }

    @Test
    void sendAppointmentReminders_marksRemindersIdempotently() {
        String targetDate = "2026-08-30";
        ClinicAppointment appt1 = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, targetDate, "10:00", 1788700000000L, 20, ClinicAppointment.Source.PHONE, "Consult");

        when(appointmentRepository.findPendingRemindersForDate(APP_ID, targetDate))
                .thenReturn(List.of(appt1));

        int sentCount = appointmentService.sendAppointmentReminders(targetDate);

        assertEquals(1, sentCount);
        assertNotNull(appt1.getReminderSentAt());
        verify(appointmentRepository).save(appt1);
    }

    @Test
    void getAppointmentMetrics_computesCorrectRates() {
        ClinicAppointment a1 = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, "2026-08-01", "09:00", 100L, 20, ClinicAppointment.Source.PHONE, null);
        a1.markDone();
        ClinicAppointment a2 = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, "2026-08-01", "09:20", 200L, 20, ClinicAppointment.Source.PHONE, null);
        a2.markDone();
        ClinicAppointment a3 = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, "2026-08-01", "09:40", 300L, 20, ClinicAppointment.Source.PHONE, null);
        a3.markNoShow();
        ClinicAppointment a4 = new ClinicAppointment(PATIENT_ID, DOCTOR_ID, "2026-08-01", "10:00", 400L, 20, ClinicAppointment.Source.PHONE, null);
        a4.cancel();

        when(appointmentRepository.findAllForDoctorInPeriod(APP_ID, DOCTOR_ID, "2026-08"))
                .thenReturn(List.of(a1, a2, a3, a4));

        AppointmentMetricsResponse metrics = appointmentService.getAppointmentMetrics(DOCTOR_ID, "2026-08");

        assertNotNull(metrics);
        assertEquals(4, metrics.totalAppointments());
        assertEquals(2, metrics.completedCount());
        assertEquals(1, metrics.noShowCount());
        assertEquals(1, metrics.cancelledCount());
        assertEquals(new BigDecimal("25.0"), metrics.noShowRatePercent()); // 1/4 = 25.0%
    }
}
