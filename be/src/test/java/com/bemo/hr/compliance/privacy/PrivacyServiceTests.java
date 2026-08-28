package com.bemo.hr.compliance.privacy;

import com.bemo.hr.compliance.privacy.application.PrivacyApi;
import com.bemo.hr.compliance.privacy.application.PrivacyService;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistry;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistryRepository;
import com.bemo.hr.compliance.privacy.domain.PrivacyRequest;
import com.bemo.hr.compliance.privacy.domain.PrivacyRequestRepository;
import com.bemo.hr.compliance.privacy.domain.RetentionPolicy;
import com.bemo.hr.compliance.privacy.domain.RetentionPolicyRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyServiceTests {

    @Mock private PrivacyRequestRepository privacyRequestRepository;
    @Mock private ConsentRegistryRepository consentRegistryRepository;
    @Mock private RetentionPolicyRepository retentionPolicyRepository;
    @Mock private com.bemo.hr.employee.infrastructure.EmployeeRepository employeeRepository;
    @Mock private com.bemo.hr.party.BusinessPartyRepository businessPartyRepository;
    @Mock private com.bemo.hr.audit.application.AuditService auditService;

    @InjectMocks
    private PrivacyService privacyService;

    private static final String TEST_APP_ID = "DEMO";

    @BeforeEach
    void setUp() { TenantContext.set(TEST_APP_ID); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test
    void createRequest_validRequest_persists() {
        var request = new PrivacyApi.CreateRequest("EMPLOYEE", "EMP-001", "EXPORT");
        when(privacyRequestRepository.save(any(PrivacyRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = privacyService.createRequest(request);

        assertEquals(PrivacyRequest.SubjectType.EMPLOYEE, result.getSubjectType());
        assertEquals("EMP-001", result.getSubjectRef());
        assertEquals(PrivacyRequest.Kind.EXPORT, result.getKind());
        assertEquals(PrivacyRequest.Status.RECEIVED, result.getStatus());
        assertNotNull(result.getDueAt());
        verify(privacyRequestRepository).save(any(PrivacyRequest.class));
    }

    @Test
    void listRequests_returnsAllForApp() {
        var pr = new PrivacyRequest(TEST_APP_ID, PrivacyRequest.SubjectType.PATIENT, "P-1", PrivacyRequest.Kind.EXPORT);
        when(privacyRequestRepository.findByAppIdOrderByCreatedAtDesc(TEST_APP_ID)).thenReturn(List.of(pr));

        var result = privacyService.listRequests();

        assertEquals(1, result.size());
    }

    @Test
    void decideRequest_complete_marksCompleted() {
        var pr = new PrivacyRequest(TEST_APP_ID, PrivacyRequest.SubjectType.EMPLOYEE, "E-1", PrivacyRequest.Kind.EXPORT);
        when(privacyRequestRepository.findById(pr.getId())).thenReturn(Optional.of(pr));
        when(privacyRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = privacyService.decideRequest(pr.getId(),
                new PrivacyApi.DecideRequest("COMPLETED", null), "admin");

        assertEquals(PrivacyRequest.Status.COMPLETED, result.getStatus());
        assertEquals("admin", result.getDecidedBy());
        assertNotNull(result.getDecidedAt());
    }

    @Test
    void decideRequest_reject_withoutNote_throws() {
        var pr = new PrivacyRequest(TEST_APP_ID, PrivacyRequest.SubjectType.EMPLOYEE, "E-1", PrivacyRequest.Kind.ERASE);
        when(privacyRequestRepository.findById(pr.getId())).thenReturn(Optional.of(pr));

        var ex = assertThrows(BusinessRuleException.class,
                () -> privacyService.decideRequest(pr.getId(),
                        new PrivacyApi.DecideRequest("REJECTED", null), "admin"));
        assertEquals("PRIVACY_REJECTION_NOTE_REQUIRED", ex.getCode());
    }

    @Test
    void decideRequest_reject_withNote_rejects() {
        var pr = new PrivacyRequest(TEST_APP_ID, PrivacyRequest.SubjectType.EMPLOYEE, "E-1", PrivacyRequest.Kind.ERASE);
        when(privacyRequestRepository.findById(pr.getId())).thenReturn(Optional.of(pr));
        when(privacyRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = privacyService.decideRequest(pr.getId(),
                new PrivacyApi.DecideRequest("REJECTED", "Legal retention"), "admin");

        assertEquals(PrivacyRequest.Status.REJECTED, result.getStatus());
        assertEquals("Legal retention", result.getLegalNote());
    }

    @Test
    void exportSubjectData_returnsJson() {
        var result = privacyService.exportSubjectData("EMPLOYEE", "EMP-001");

        assertTrue(result.contains("EMPLOYEE"));
        assertTrue(result.contains("EMP-001"));
        assertTrue(result.contains("exportedAt"));
    }

    @Test
    void eraseSubjectData_returnsRetainedTypes() {
        var result = privacyService.eraseSubjectData("EMPLOYEE", "EMP-001", "admin");

        assertNotNull(result);
        assertTrue(result.contains("EMPLOYEE_FINANCIAL_RECORDS"));
    }

    @Test
    void grantConsent_persistsConsent() {
        var request = new PrivacyApi.ConsentRequest("EMP-001", "EMPLOYEE", "marketing");
        when(consentRegistryRepository.save(any(ConsentRegistry.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = privacyService.grantConsent(request);

        assertEquals("EMP-001", result.getSubjectRef());
        assertEquals("marketing", result.getPurposeKey());
        assertTrue(result.isActive());
    }

    @Test
    void withdrawConsent_setsWithdrawnAt() {
        var consent = new ConsentRegistry(TEST_APP_ID, "EMP-001", "EMPLOYEE", "marketing");
        when(consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull(TEST_APP_ID, "EMP-001"))
                .thenReturn(List.of(consent));
        when(consentRegistryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = privacyService.withdrawConsent(
                new PrivacyApi.ConsentWithdrawRequest("EMP-001", "marketing"));

        assertFalse(result.isActive());
        assertNotNull(result.getWithdrawnAt());
    }

    @Test
    void withdrawConsent_noActiveConsent_throws() {
        when(consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull(TEST_APP_ID, "EMP-001"))
                .thenReturn(List.of());

        assertThrows(com.bemo.hr.shared.domain.NotFoundException.class,
                () -> privacyService.withdrawConsent(
                        new PrivacyApi.ConsentWithdrawRequest("EMP-001", "nonexistent")));
    }

    @Test
    void createRetentionPolicy_persists() {
        var request = new PrivacyApi.RetentionPolicyRequest("punch_records", 24, "ANONYMIZE");
        when(retentionPolicyRepository.save(any(RetentionPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = privacyService.createRetentionPolicy(request);

        assertEquals("punch_records", result.getEntityKey());
        assertEquals(24, result.getMonths());
        assertEquals(RetentionPolicy.Action.ANONYMIZE, result.getAction());
        assertTrue(result.isActive());
    }

    @Test
    void dryRunRetention_returnsResultsPerPolicy() {
        var policy = new RetentionPolicy(TEST_APP_ID, "punch_records", 12, RetentionPolicy.Action.DELETE);
        when(retentionPolicyRepository.findByAppIdAndActiveTrue(TEST_APP_ID)).thenReturn(List.of(policy));

        var results = privacyService.dryRunRetention();

        assertEquals(1, results.size());
        assertEquals("punch_records", results.get(0).entityKey());
        assertEquals("DELETE", results.get(0).action());
    }

    @Test
    void responseFrom_mapsOverdue() {
        var pr = new PrivacyRequest(TEST_APP_ID, PrivacyRequest.SubjectType.EMPLOYEE, "E-1", PrivacyRequest.Kind.EXPORT);
        var response = PrivacyApi.Response.from(pr);

        assertFalse(response.overdue());
        assertEquals("RECEIVED", response.status());
    }

    @Test
    void exportSubjectData_gathersEmployeePiiAndNeverLeaksOtherSubjects() {
        Employee subject = new Employee("EMP-001", "Ahmed Ali", "dev-1", "cat-1",
                EmploymentType.FIXED, LocalDate.now(), null, true);
        when(employeeRepository.findByEmployeeCodeIgnoreCase("EMP-001")).thenReturn(Optional.of(subject));

        var json = privacyService.exportSubjectData("EMPLOYEE", "EMP-001");

        assertTrue(json.contains("\"employeeCode\":\"EMP-001\""));
        assertTrue(json.contains("\"fullName\":\"Ahmed Ali\""));
        assertFalse(json.contains("Mohamed Other"), "No foreign subject PII may leak into the bundle");
        assertFalse(json.contains("\"party\":"), "Party data must not leak into an employee export");
        assertTrue(json.contains("\"attachments\":[]"));
        verify(auditService).record(eq("PRIVACY_EXPORT"), eq("PRIVACY_REQUEST"), eq("EMP-001"), any(), any(), isNull());
    }

    @Test
    void exportSubjectData_partySubject_gathersPartyPii() {
        var party = new com.bemo.hr.party.BusinessParty(
                "P-9", "شركة النور", "Al Noor Co", "SUPPLIER", null,
                "+201000", "info@noor.example", "Cairo", null, true,
                null, null, null, null, "EGP", null, null, "310-123-456", null);
        when(businessPartyRepository.findByCodeIgnoreCase("P-9")).thenReturn(Optional.of(party));

        var json = privacyService.exportSubjectData("PARTY", "P-9");

        assertTrue(json.contains("\"taxId\":\"310-123-456\""));
        assertTrue(json.contains("\"nameEn\":\"Al Noor Co\""));
        assertFalse(json.contains("\"employee\":"));
    }

    @Test
    void dryRunRetention_countsRealRowsPerPolicy() {
        var empPolicy = new RetentionPolicy(TEST_APP_ID, "EMPLOYEE", 24, RetentionPolicy.Action.ANONYMIZE);
        var partyPolicy = new RetentionPolicy(TEST_APP_ID, "PARTY", 12, RetentionPolicy.Action.DELETE);
        when(retentionPolicyRepository.findByAppIdAndActiveTrue(TEST_APP_ID)).thenReturn(List.of(empPolicy, partyPolicy));
        when(employeeRepository.countByAppIdAndCreatedAtBefore(eq(TEST_APP_ID), any(Instant.class))).thenReturn(3L);
        when(businessPartyRepository.countByAppIdAndCreatedAtBefore(eq(TEST_APP_ID), any(Instant.class))).thenReturn(5L);

        var results = privacyService.dryRunRetention();

        assertEquals(2, results.size());
        assertEquals(3L, results.get(0).affectedCount());
        assertEquals("ANONYMIZE", results.get(0).action());
        assertEquals(5L, results.get(1).affectedCount());
    }

    @Test
    void createRequest_auditsCreation() {
        var request = new PrivacyApi.CreateRequest("EMPLOYEE", "EMP-001", "EXPORT");
        when(privacyRequestRepository.save(any(PrivacyRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        privacyService.createRequest(request);

        verify(auditService).record(eq("PRIVACY_CREATE_REQUEST"), eq("PRIVACY_REQUEST"), any(), any(), any(), isNull());
    }

    @Test
    void decideRequest_auditsDecision() {
        var pr = new PrivacyRequest(TEST_APP_ID, PrivacyRequest.SubjectType.EMPLOYEE, "E-1", PrivacyRequest.Kind.ERASE);
        when(privacyRequestRepository.findById(pr.getId())).thenReturn(Optional.of(pr));
        when(privacyRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        privacyService.decideRequest(pr.getId(), new PrivacyApi.DecideRequest("COMPLETED", null), "admin");

        verify(auditService).record(eq("PRIVACY_DECIDE_REQUEST"), eq("PRIVACY_REQUEST"), eq(pr.getId()), eq("admin"), any(), isNull());
    }

    @Test
    void withdrawConsent_auditsWithdrawal() {
        var consent = new ConsentRegistry(TEST_APP_ID, "EMP-001", "EMPLOYEE", "marketing");
        when(consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull(TEST_APP_ID, "EMP-001"))
                .thenReturn(List.of(consent));
        when(consentRegistryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        privacyService.withdrawConsent(new PrivacyApi.ConsentWithdrawRequest("EMP-001", "marketing"));

        verify(auditService).record(eq("PRIVACY_WITHDRAW_CONSENT"), eq("CONSENT_REGISTRY"), eq(consent.getId()), any(), any(), isNull());
    }
}
