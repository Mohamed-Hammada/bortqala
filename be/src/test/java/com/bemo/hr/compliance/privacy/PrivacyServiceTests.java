package com.bemo.hr.compliance.privacy;

import com.bemo.hr.compliance.privacy.application.PrivacyApi;
import com.bemo.hr.compliance.privacy.application.PrivacyService;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistry;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistryRepository;
import com.bemo.hr.compliance.privacy.domain.PrivacyRequest;
import com.bemo.hr.compliance.privacy.domain.PrivacyRequestRepository;
import com.bemo.hr.compliance.privacy.domain.RetentionPolicy;
import com.bemo.hr.compliance.privacy.domain.RetentionPolicyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
