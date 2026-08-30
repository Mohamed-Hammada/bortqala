package com.bemo.hr.medical;

import com.bemo.hr.medical.api.MedicalClinicApi.*;
import com.bemo.hr.medical.application.HospitalOpsService;
import com.bemo.hr.medical.domain.*;
import com.bemo.hr.medical.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HospitalOpsServiceTests {

    @Mock
    private HospitalWardRepository wardRepository;
    @Mock
    private HospitalRoomRepository roomRepository;
    @Mock
    private HospitalBedRepository bedRepository;
    @Mock
    private HospitalAdmissionRepository admissionRepository;
    @Mock
    private HospitalBedStayRepository bedStayRepository;
    @Mock
    private HospitalOtScheduleRepository otScheduleRepository;
    @Mock
    private HospitalOtChargeRepository otChargeRepository;
    @Mock
    private HospitalMarEntryRepository marEntryRepository;
    @Mock
    private HospitalFluidIoEntryRepository fluidIoEntryRepository;
    @Mock
    private HospitalNursingNoteRepository nursingNoteRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRosterRepository doctorRosterRepository;

    private HospitalOpsService hospitalOpsService;

    private final String APP_ID = "tenant-test";

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
        hospitalOpsService = new HospitalOpsService(
                wardRepository,
                roomRepository,
                bedRepository,
                admissionRepository,
                bedStayRepository,
                otScheduleRepository,
                otChargeRepository,
                marEntryRepository,
                fluidIoEntryRepository,
                nursingNoteRepository,
                patientRepository,
                doctorRosterRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void admitPatientToFreeBed_SuccessAndOccupiesBed() {
        Patient patient = new Patient("MRN-001", "29501011234567", "Amr Hassan", "01001234567", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        patient.setId("pat-1");

        HospitalBed bed = new HospitalBed("room-1", "B-101", HospitalBed.Status.FREE);
        bed.setId("bed-1");

        when(patientRepository.findByAppIdAndId(APP_ID, "pat-1")).thenReturn(Optional.of(patient));
        when(admissionRepository.findByAppIdAndPatientIdAndStatus(APP_ID, "pat-1", HospitalAdmission.Status.ADMITTED))
                .thenReturn(Optional.empty());
        when(bedRepository.findByAppIdAndId(APP_ID, "bed-1")).thenReturn(Optional.of(bed));
        when(admissionRepository.save(any(HospitalAdmission.class))).thenAnswer(i -> i.getArgument(0));
        when(bedRepository.save(any(HospitalBed.class))).thenAnswer(i -> i.getArgument(0));
        when(bedStayRepository.save(any(HospitalBedStay.class))).thenAnswer(i -> i.getArgument(0));

        AdmitPatientRequest request = new AdmitPatientRequest("pat-1", "doc-1", "bed-1", "Acute chest pain");
        HospitalAdmissionDto result = hospitalOpsService.admitPatient(request);

        assertThat(result).isNotNull();
        assertThat(result.patientId()).isEqualTo("pat-1");
        assertThat(result.status()).isEqualTo("ADMITTED");
        assertThat(bed.getStatus()).isEqualTo(HospitalBed.Status.OCCUPIED);
    }

    @Test
    void admitPatientToOccupiedBed_ThrowsBedNotFree() {
        Patient patient = new Patient("MRN-001", "29501011234567", "Amr Hassan", "01001234567", "MALE", "1995-01-01", "O_POS", null, null, null, null);
        patient.setId("pat-1");

        HospitalBed bed = new HospitalBed("room-1", "B-101", HospitalBed.Status.OCCUPIED);
        bed.setId("bed-1");

        when(patientRepository.findByAppIdAndId(APP_ID, "pat-1")).thenReturn(Optional.of(patient));
        when(admissionRepository.findByAppIdAndPatientIdAndStatus(APP_ID, "pat-1", HospitalAdmission.Status.ADMITTED))
                .thenReturn(Optional.empty());
        when(bedRepository.findByAppIdAndId(APP_ID, "bed-1")).thenReturn(Optional.of(bed));

        AdmitPatientRequest request = new AdmitPatientRequest("pat-1", "doc-1", "bed-1", "Acute chest pain");

        assertThatThrownBy(() -> hospitalOpsService.admitPatient(request))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("BED_NOT_FREE");
    }

    @Test
    void transferPatient_AtomicallyFreesOldBedAndOccupiesNewBed() {
        HospitalAdmission admission = new HospitalAdmission("pat-1", "doc-1", "bed-1", "Pneumonia");
        admission.setId("adm-1");

        HospitalBed oldBed = new HospitalBed("room-1", "B-101", HospitalBed.Status.OCCUPIED);
        oldBed.setId("bed-1");

        HospitalBed targetBed = new HospitalBed("room-2", "ICU-01", HospitalBed.Status.FREE);
        targetBed.setId("bed-2");

        HospitalBedStay oldStay = new HospitalBedStay("adm-1", "bed-1", "Initial");
        oldStay.setId("stay-1");

        when(admissionRepository.findByAppIdAndId(APP_ID, "adm-1")).thenReturn(Optional.of(admission));
        when(bedRepository.findByAppIdAndId(APP_ID, "bed-2")).thenReturn(Optional.of(targetBed));
        when(bedRepository.findByAppIdAndId(APP_ID, "bed-1")).thenReturn(Optional.of(oldBed));
        when(bedStayRepository.findByAppIdAndAdmissionIdAndEndedAtIsNull(APP_ID, "adm-1")).thenReturn(Optional.of(oldStay));
        when(bedRepository.save(any(HospitalBed.class))).thenAnswer(i -> i.getArgument(0));
        when(bedStayRepository.save(any(HospitalBedStay.class))).thenAnswer(i -> i.getArgument(0));
        when(admissionRepository.save(any(HospitalAdmission.class))).thenAnswer(i -> i.getArgument(0));

        TransferPatientBedRequest request = new TransferPatientBedRequest("bed-2", "Condition deteriorated, transferred to ICU");
        HospitalAdmissionDto result = hospitalOpsService.transferPatient("adm-1", request);

        assertThat(result).isNotNull();
        assertThat(oldBed.getStatus()).isEqualTo(HospitalBed.Status.FREE);
        assertThat(targetBed.getStatus()).isEqualTo(HospitalBed.Status.OCCUPIED);
        assertThat(oldStay.getEndedAt()).isNotNull();
        assertThat(admission.getCurrentBedId()).isEqualTo("bed-2");
    }

    @Test
    void dischargePatient_ValidatesSummaryLengthAndFreesBed() {
        HospitalAdmission admission = new HospitalAdmission("pat-1", "doc-1", "bed-1", "Pneumonia");
        admission.setId("adm-1");

        HospitalBed bed = new HospitalBed("room-1", "B-101", HospitalBed.Status.OCCUPIED);
        bed.setId("bed-1");

        HospitalBedStay stay = new HospitalBedStay("adm-1", "bed-1", "Initial");

        when(admissionRepository.findByAppIdAndId(APP_ID, "adm-1")).thenReturn(Optional.of(admission));

        // Short summary should fail
        DischargePatientRequest shortReq = new DischargePatientRequest("Too short");
        assertThatThrownBy(() -> hospitalOpsService.dischargePatient("adm-1", shortReq))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode())
                .isEqualTo("DISCHARGE_SUMMARY_REQUIRED");

        // Proper summary should succeed
        when(bedRepository.findByAppIdAndId(APP_ID, "bed-1")).thenReturn(Optional.of(bed));
        when(bedStayRepository.findByAppIdAndAdmissionIdAndEndedAtIsNull(APP_ID, "adm-1")).thenReturn(Optional.of(stay));
        when(admissionRepository.save(any(HospitalAdmission.class))).thenAnswer(i -> i.getArgument(0));

        DischargePatientRequest validReq = new DischargePatientRequest("Patient fully recovered and discharged home with oral antibiotics.");
        HospitalAdmissionDto result = hospitalOpsService.dischargePatient("adm-1", validReq);

        assertThat(result).isNotNull();
        assertThat(admission.getStatus()).isEqualTo(HospitalAdmission.Status.DISCHARGED);
        assertThat(bed.getStatus()).isEqualTo(HospitalBed.Status.FREE);
    }

    @Test
    void marAdministration_TransitionsStateAndRecordsNurse() {
        HospitalMarEntry entry = new HospitalMarEntry("adm-1", "Ceftriaxone 1g IV", "1g", "IV", System.currentTimeMillis());
        entry.setId("mar-1");

        when(marEntryRepository.findByAppIdAndId(APP_ID, "mar-1")).thenReturn(Optional.of(entry));
        when(marEntryRepository.save(any(HospitalMarEntry.class))).thenAnswer(i -> i.getArgument(0));

        AdministerMarEntryRequest req = new AdministerMarEntryRequest("GIVEN", "nurse-1", "Nurse Sara", "Administered via IV slow push");
        HospitalMarEntryDto result = hospitalOpsService.administerMarEntry("mar-1", req);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("GIVEN");
        assertThat(result.nurseName()).isEqualTo("Nurse Sara");
        assertThat(result.administeredAt()).isNotNull();
    }

    @Test
    void occupancyMetrics_CalculatesTotalOccupiedAndAlos() {
        when(bedRepository.countTotalActiveBeds(APP_ID)).thenReturn(50L);
        when(bedRepository.countOccupiedBeds(APP_ID)).thenReturn(35L);

        HospitalAdmission discharged = new HospitalAdmission("pat-1", "doc-1", null, "Fever");
        discharged.setAdmittedAt(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L); // 3 days ago
        discharged.setDischargedAt(System.currentTimeMillis());
        discharged.setStatus(HospitalAdmission.Status.DISCHARGED);

        when(admissionRepository.findAllByAppIdAndStatusOrderByAdmittedAtDesc(APP_ID, HospitalAdmission.Status.DISCHARGED))
                .thenReturn(List.of(discharged));

        HospitalOccupancyMetricsDto metrics = hospitalOpsService.getOccupancyMetrics();

        assertThat(metrics.totalBeds()).isEqualTo(50L);
        assertThat(metrics.occupiedBeds()).isEqualTo(35L);
        assertThat(metrics.occupancyRatePercent()).isEqualTo(70.0);
        assertThat(metrics.averageLengthOfStayDays()).isGreaterThanOrEqualTo(2.9);
    }
}
