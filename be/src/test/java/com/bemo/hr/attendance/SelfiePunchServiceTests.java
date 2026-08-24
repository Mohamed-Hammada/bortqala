package com.bemo.hr.attendance;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.attendance.api.SelfiePunchApi;
import com.bemo.hr.attendance.application.SelfiePunchService;
import com.bemo.hr.attendance.domain.AttendanceSelfiePunch;
import com.bemo.hr.attendance.infrastructure.AttendanceSelfiePunchRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfiePunchServiceTests {

    private AttendanceSelfiePunchRepository punchRepository;
    private EmployeeRepository employeeRepository;
    private AuditService auditService;
    private Employee employee;
    private SelfiePunchService service;

    @BeforeEach
    void setUp() {
        punchRepository = mock(AttendanceSelfiePunchRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        auditService = mock(AuditService.class);
        employee = employee("emp-1");
        service = new SelfiePunchService(punchRepository, employeeRepository, auditService);
        when(employeeRepository.findByDeviceUserId("merl")).thenReturn(Optional.of(employee));
        when(punchRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Employee employee(String id) {
        return new Employee("EMP-1", "Merl", "merl", "cat-1",
                com.bemo.hr.employee.domain.EmploymentType.FIXED,
                java.math.BigDecimal.ZERO,
                java.time.LocalDate.of(2026, 1, 1), null, true);
    }

    private SelfiePunchApi.SelfiePunchRequest request(String operationId) {
        String image = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});
        return new SelfiePunchApi.SelfiePunchRequest(operationId, 1_700_000_000_000L, "image/jpeg",
                4, image);
    }

    @Test
    void firstPunchStoresServerTimestampAndAudits() {
        long before = System.currentTimeMillis();
        SelfiePunchApi.SelfiePunchResponse response = service.punch("merl", request("op-1"));

        assertThat(response.duplicate()).isFalse();
        assertThat(response.employeeId()).isEqualTo(employee.getId());
        assertThat(response.punchedAt()).isGreaterThanOrEqualTo(before);
        verify(auditService).record(eq("CREATE"), eq("ATT_SELFIE_PUNCH"), anyString(), eq("merl"), anyString(), any());
    }

    @Test
    void replayReturnsOriginalExactlyOnceWithoutSecondRow() {
        AttendanceSelfiePunch stored = new AttendanceSelfiePunch(employee.getId(), "op-1", null, "image/jpeg", "x");
        when(punchRepository.findByOperationId("op-1")).thenReturn(Optional.of(stored));

        SelfiePunchApi.SelfiePunchResponse response = service.punch("merl", request("op-1"));

        assertThat(response.duplicate()).isTrue();
        assertThat(response.id()).isEqualTo(stored.getId());
        verify(punchRepository, never()).save(any());
        verify(auditService, never()).record(anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void operationOwnedByAnotherEmployeeConflicts() {
        AttendanceSelfiePunch stored = new AttendanceSelfiePunch("some-other-employee", "op-1", null, "image/jpeg", "x");
        when(punchRepository.findByOperationId("op-1")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.punch("merl", request("op-1")))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ATT_SELFIE_OPERATION_TAKEN"));
    }

    @Test
    void userWithoutEmployeeLinkIsRejected() {
        when(employeeRepository.findByDeviceUserId("nolink")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.punch("nolink", request("op-9")))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ATT_SELFIE_EMPLOYEE_NOT_LINKED"));
    }

    @Test
    void oversizedOrMalformedImageRejected() {
        String huge = Base64.getEncoder().encodeToString(new byte[SelfiePunchService.MAX_IMAGE_BYTES + 1]);
        SelfiePunchApi.SelfiePunchRequest tooLarge = new SelfiePunchApi.SelfiePunchRequest(
                "op-big", null, "image/jpeg", huge.length(), huge);
        assertThatThrownBy(() -> service.punch("merl", tooLarge))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ATT_SELFIE_IMAGE_TOO_LARGE"));

        SelfiePunchApi.SelfiePunchRequest malformed = new SelfiePunchApi.SelfiePunchRequest(
                "op-bad", null, "image/jpeg", null, "@@@not-base64@@@");
        assertThatThrownBy(() -> service.punch("merl", malformed))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ATT_SELFIE_IMAGE_INVALID"));
    }

    @Test
    void declaredSizeMismatchRejected() {
        SelfiePunchApi.SelfiePunchRequest mismatched = new SelfiePunchApi.SelfiePunchRequest(
                "op-size", null, "image/jpeg", 99,
                Base64.getEncoder().encodeToString(new byte[]{9, 9}));
        assertThatThrownBy(() -> service.punch("merl", mismatched))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("ATT_SELFIE_IMAGE_INVALID"));
        verify(punchRepository, never()).save(any());
    }
}
