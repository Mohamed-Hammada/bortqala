package com.bemo.hr.approval;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceTests {
    @Mock
    private ApprovalWorkflowDefinitionRepository definitionRepository;
    @Mock
    private ApprovalWorkflowStepRepository stepRepository;
    @Mock
    private ApprovalInstanceRepository instanceRepository;
    @Mock
    private ApprovalInstanceStepRepository instanceStepRepository;
    @Mock
    private ApprovalDecisionRepository decisionRepository;
    @Mock
    private ApprovalDelegationRepository delegationRepository;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private ApprovalWorkflowService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitFailsWhenNoMatchingDefinition() {
        when(definitionRepository.findByDocumentTypeAndActiveTrue("PURCHASE_ORDER")).thenReturn(List.of());
        assertThatThrownBy(() -> service.submit(new ApprovalApi.SubmitDocumentRequest("PURCHASE_ORDER", "PO-100", new BigDecimal("5000"))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("APPROVAL_WORKFLOW_NOT_FOUND");
    }

    @Test
    void rejectFailsWhenCommentIsBlank() {
        assertThatThrownBy(() -> service.reject(new ApprovalApi.DecisionRequest("inst-1", "   ")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("APPROVAL_REJECTION_REASON_REQUIRED");
    }

    @Test
    void createWorkflowDefinitionSavesValidatedAnyNPolicy() {
        ApprovalApi.StepRequest step = new ApprovalApi.StepRequest(1, "STEP_1", "Manager", "PROCUREMENT_MANAGER",
                null, null, null, 2, false, 24, "ANY_N");
        ApprovalWorkflowDefinition saved = new ApprovalWorkflowDefinition("PURCHASE_ORDER", "PO approvals", true);
        when(definitionRepository.save(any())).thenReturn(saved);
        ApprovalApi.WorkflowDefinitionResponse response = service.createWorkflowDefinition(
                new ApprovalApi.WorkflowDefinitionRequest("PURCHASE_ORDER", "PO approvals", true, List.of(step)));
        ArgumentCaptor<ApprovalWorkflowStep> captor = ArgumentCaptor.forClass(ApprovalWorkflowStep.class);
        verify(stepRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionPolicy()).isEqualTo("ANY_N");
        assertThat(captor.getValue().getMinimumApprovals()).isEqualTo(2);
        assertThat(response.documentType()).isEqualTo("PURCHASE_ORDER");
    }

    @Test
    void anyNDoesNotAdvanceUntilThresholdIsReached() {
        authenticate("manager", "PROCUREMENT_MANAGER");
        ApprovalInstance instance = instance();
        ApprovalInstanceStep snapshot = snapshot(instance, 2, "PROCUREMENT_MANAGER", null, false);
        stubActive(instance, snapshot);
        when(decisionRepository.countByInstanceIdAndStepIdAndDecision(instance.getId(), snapshot.getId(), "APPROVED"))
                .thenReturn(1L, 1L);

        ApprovalApi.ApprovalInstanceDetailResponse detail = service.approve(new ApprovalApi.DecisionRequest(instance.getId(), "ok"));

        assertThat(detail.approvalsReceived()).isEqualTo(1);
        assertThat(instance.getStatus()).isEqualTo("SUBMITTED");
        verify(instanceRepository, never()).save(instance);
    }

    @Test
    void delegatedDecisionRecordsOriginalApproverAndCompletesThreshold() {
        authenticate("backup", "USER");
        ApprovalInstance instance = instance();
        ApprovalInstanceStep snapshot = snapshot(instance, 1, null, "owner", false);
        stubActive(instance, snapshot);
        ApprovalDelegation delegation = new ApprovalDelegation("owner", "backup", "PURCHASE_ORDER",
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), "leave", "owner");
        when(delegationRepository.findByDelegateUserIdIgnoreCaseAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of(delegation));
        when(decisionRepository.countByInstanceIdAndStepIdAndDecision(instance.getId(), snapshot.getId(), "APPROVED"))
                .thenReturn(1L, 1L);

        service.approve(new ApprovalApi.DecisionRequest(instance.getId(), null));

        ArgumentCaptor<ApprovalDecision> captor = ArgumentCaptor.forClass(ApprovalDecision.class);
        verify(decisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDelegatedFrom()).isEqualTo("owner");
        assertThat(instance.getStatus()).isEqualTo("APPROVED");
        verify(stepRepository, never()).findByWorkflowDefinitionIdAndStepOrder(any(), anyInt());
    }

    @Test
    void selfApprovalIsBlockedBySnapshotRule() {
        authenticate("submitter", "PROCUREMENT_MANAGER");
        ApprovalInstance instance = instance();
        ApprovalInstanceStep snapshot = snapshot(instance, 1, "PROCUREMENT_MANAGER", null, false);
        when(instanceRepository.findByIdForUpdate(instance.getId())).thenReturn(Optional.of(instance));
        when(instanceStepRepository.findByInstanceIdAndStepOrder(instance.getId(), 1)).thenReturn(Optional.of(snapshot));
        assertThatThrownBy(() -> service.approve(new ApprovalApi.DecisionRequest(instance.getId(), null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("APPROVAL_SELF_APPROVAL_BLOCKED");
    }

    @Test
    void delegationRejectsInvalidDateRange() {
        authenticate("owner", "USER");
        long now = System.currentTimeMillis();
        assertThatThrownBy(() -> service.createDelegation(new ApprovalApi.DelegationRequest(
                "owner", "backup", null, now, now, "leave")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("APPROVAL_DELEGATION_DATES_INVALID");
    }

    @Test
    void overdueScanEscalatesAnInstanceOnlyThroughTheLockedQuery() {
        ApprovalInstance instance = instance();
        ReflectionTestUtils.setField(instance, "stepDueAt", Instant.now().minusSeconds(60));
        when(instanceRepository.findByStatusInAndStepDueAtBeforeAndEscalatedAtIsNull(any(), any())).thenReturn(List.of(instance));
        assertThat(service.escalateOverdue()).isEqualTo(1);
        assertThat(instance.getEscalationLevel()).isEqualTo(1);
        assertThat(instance.getEscalatedAt()).isNotNull();
        verify(instanceRepository).save(instance);
    }

    private void stubActive(ApprovalInstance instance, ApprovalInstanceStep snapshot) {
        when(instanceRepository.findByIdForUpdate(instance.getId())).thenReturn(Optional.of(instance));
        when(instanceRepository.findByDocumentTypeAndDocumentId("PURCHASE_ORDER", "doc-1")).thenReturn(Optional.of(instance));
        when(instanceStepRepository.findByInstanceIdAndStepOrder(instance.getId(), 1)).thenReturn(Optional.of(snapshot));
        when(decisionRepository.findByInstanceIdOrderByDecidedAtAsc(instance.getId())).thenReturn(List.of());
    }

    private ApprovalInstance instance() {
        ApprovalInstance instance = new ApprovalInstance("def-1", 7, "PURCHASE_ORDER", "doc-1", "submitter", "{\"total\":100}", 24);
        instance.prePersist();
        return instance;
    }

    private ApprovalInstanceStep snapshot(ApprovalInstance instance, int minimum, String role, String user, boolean self) {
        ApprovalWorkflowStep source = new ApprovalWorkflowStep("def-1", 1, "STEP_1", "Manager", role, user,
                null, null, minimum, self, 24, "ANY_N");
        return new ApprovalInstanceStep(instance.getId(), source);
    }

    private void authenticate(String username, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, "n/a",
                java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
    }
}
