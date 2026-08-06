package com.bemo.hr.approval;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceTests {

    @Mock private ApprovalWorkflowDefinitionRepository definitionRepository;
    @Mock private ApprovalWorkflowStepRepository stepRepository;
    @Mock private ApprovalInstanceRepository instanceRepository;
    @Mock private ApprovalDecisionRepository decisionRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ApprovalWorkflowService service;

    @Test
    void submitFailsWhenNoMatchingDefinition() {
        when(definitionRepository.findByDocumentTypeAndActiveTrue("PURCHASE_ORDER")).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(new ApprovalApi.SubmitDocumentRequest("PURCHASE_ORDER", "PO-100", new BigDecimal("5000"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("مسار اعتماد");
    }

    @Test
    void rejectFailsWhenCommentIsBlank() {
        assertThatThrownBy(() -> service.reject(new ApprovalApi.DecisionRequest("inst-1", "   ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("سبب الرفض إجباري");
    }

    @Test
    void createWorkflowDefinitionSavesStepsAndAudits() {
        ApprovalApi.StepRequest step1 = new ApprovalApi.StepRequest(1, "STEP_1", "مدير المشتريات", "PROCUREMENT_MANAGER", null, null, null, 1, false, 24);
        ApprovalApi.WorkflowDefinitionRequest request = new ApprovalApi.WorkflowDefinitionRequest("PURCHASE_ORDER", "مسار أمر الشراء", true, List.of(step1));

        ApprovalWorkflowDefinition savedDef = new ApprovalWorkflowDefinition("PURCHASE_ORDER", "مسار أمر الشراء", true);
        when(definitionRepository.save(any(ApprovalWorkflowDefinition.class))).thenReturn(savedDef);

        ApprovalApi.WorkflowDefinitionResponse response = service.createWorkflowDefinition(request);

        assertThat(response.name()).isEqualTo("مسار أمر الشراء");
        assertThat(response.documentType()).isEqualTo("PURCHASE_ORDER");
    }
}
