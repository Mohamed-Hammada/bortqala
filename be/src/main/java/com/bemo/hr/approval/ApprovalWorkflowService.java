package com.bemo.hr.approval;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {
    private final ApprovalWorkflowDefinitionRepository definitionRepository;
    private final ApprovalWorkflowStepRepository stepRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ApprovalApi.WorkflowDefinitionResponse> listWorkflowDefinitions() {
        return definitionRepository.findAll().stream()
                .sorted(Comparator.comparing(ApprovalWorkflowDefinition::getDocumentType))
                .map(this::mapDefinitionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalApi.WorkflowDefinitionResponse getWorkflowDefinition(String id) {
        ApprovalWorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("مسار الاعتماد غير موجود: " + id, "APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapDefinitionToResponse(def);
    }

    @Transactional
    public ApprovalApi.WorkflowDefinitionResponse createWorkflowDefinition(ApprovalApi.WorkflowDefinitionRequest request) {
        ApprovalWorkflowDefinition def = new ApprovalWorkflowDefinition(request.documentType(), request.name(), request.active());
        def = definitionRepository.save(def);

        saveSteps(def.getId(), request.steps());
        auditService.record("CREATE", "APPROVAL_WORKFLOW_DEFINITION", def.getId(), actor(),
                "{\"documentType\":\"" + def.getDocumentType() + "\"}", null);

        return mapDefinitionToResponse(def);
    }

    @Transactional
    public ApprovalApi.WorkflowDefinitionResponse updateWorkflowDefinition(String id, ApprovalApi.WorkflowDefinitionRequest request) {
        ApprovalWorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("مسار الاعتماد غير موجود: " + id, "APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.NOT_FOUND));

        def.update(request.name(), request.active());
        definitionRepository.save(def);

        stepRepository.deleteByWorkflowDefinitionId(id);
        saveSteps(id, request.steps());

        auditService.record("UPDATE", "APPROVAL_WORKFLOW_DEFINITION", id, actor(),
                "{\"version\":" + def.getVersion() + "}", null);

        return mapDefinitionToResponse(def);
    }

    @Transactional
    public ApprovalApi.ApprovalInstanceDetailResponse submit(ApprovalApi.SubmitDocumentRequest request) {
        List<ApprovalWorkflowDefinition> defs = definitionRepository.findByDocumentTypeAndActiveTrue(request.documentType());
        if (defs.isEmpty()) {
            throw new BusinessRuleException("لم يتم العثور على مسار اعتماد مطبق لنوع المستند.", "APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        ApprovalWorkflowDefinition def = defs.get(0);
        List<ApprovalWorkflowStep> steps = stepRepository.findByWorkflowDefinitionIdOrderByStepOrderAsc(def.getId());
        if (steps.isEmpty()) {
            throw new BusinessRuleException("مسار الاعتماد لا يحتوي على خطوات معرفة.", "APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.CONFLICT);
        }

        String currentActor = actor();
        Optional<ApprovalInstance> existing = instanceRepository.findByDocumentTypeAndDocumentId(request.documentType(), request.documentId());
        ApprovalInstance instance;
        if (existing.isPresent()) {
            instance = existing.get();
            if ("APPROVED".equals(instance.getStatus())) {
                throw new BusinessRuleException("تم اعتماد المستند سابقاً بالكامل.", "APPROVAL_ALREADY_APPROVED", HttpStatus.CONFLICT);
            }
            instance.advanceStep(1);
        } else {
            instance = new ApprovalInstance(def.getId(), request.documentType(), request.documentId(), currentActor);
        }
        instance = instanceRepository.save(instance);

        auditService.record("SUBMIT", "APPROVAL_INSTANCE", instance.getId(), currentActor,
                "{\"documentId\":\"" + request.documentId() + "\"}", null);

        return getHistory(request.documentType(), request.documentId());
    }

    @Transactional
    public ApprovalApi.ApprovalInstanceDetailResponse approve(ApprovalApi.DecisionRequest request) {
        ApprovalInstance instance = requireActiveInstance(request.instanceId());
        ApprovalWorkflowStep currentStep = requireCurrentStep(instance);
        String currentActor = actor();

        if (!currentStep.isAllowSelfApproval() && currentActor.equalsIgnoreCase(instance.getSubmittedBy())) {
            throw new BusinessRuleException("غير مسموح باعتماد مستنداتك الخاصة وفقاً لسياسة الاعتمادات.", "APPROVAL_SELF_APPROVAL_BLOCKED", HttpStatus.CONFLICT);
        }

        validateUserAuthorization(currentStep, currentActor);

        ApprovalDecision decision = new ApprovalDecision(instance.getId(), currentStep.getId(), "APPROVED", request.comment(), currentActor, null);
        decisionRepository.save(decision);

        List<ApprovalWorkflowStep> steps = stepRepository.findByWorkflowDefinitionIdOrderByStepOrderAsc(instance.getWorkflowDefinitionId());
        int nextOrder = instance.getCurrentStepOrder() + 1;
        Optional<ApprovalWorkflowStep> nextStep = steps.stream().filter(s -> s.getStepOrder() == nextOrder).findFirst();

        if (nextStep.isPresent()) {
            instance.advanceStep(nextOrder);
        } else {
            instance.approve();
        }
        instanceRepository.save(instance);

        auditService.record("APPROVE_STEP", "APPROVAL_INSTANCE", instance.getId(), currentActor,
                "{\"stepOrder\":" + currentStep.getStepOrder() + ",\"status\":\"" + instance.getStatus() + "\"}", null);

        return getHistory(instance.getDocumentType(), instance.getDocumentId());
    }

    @Transactional
    public ApprovalApi.ApprovalInstanceDetailResponse reject(ApprovalApi.DecisionRequest request) {
        if (request.comment() == null || request.comment().isBlank()) {
            throw new BusinessRuleException("سبب الرفض إجباري عند رفض أي مستند.", "APPROVAL_REJECTION_REASON_REQUIRED", HttpStatus.BAD_REQUEST);
        }

        ApprovalInstance instance = requireActiveInstance(request.instanceId());
        ApprovalWorkflowStep currentStep = requireCurrentStep(instance);
        String currentActor = actor();

        validateUserAuthorization(currentStep, currentActor);

        ApprovalDecision decision = new ApprovalDecision(instance.getId(), currentStep.getId(), "REJECTED", request.comment().strip(), currentActor, null);
        decisionRepository.save(decision);

        instance.reject();
        instanceRepository.save(instance);

        auditService.record("REJECT_STEP", "APPROVAL_INSTANCE", instance.getId(), currentActor,
                "{\"comment\":\"" + request.comment().strip() + "\"}", null);

        return getHistory(instance.getDocumentType(), instance.getDocumentId());
    }

    @Transactional(readOnly = true)
    public List<ApprovalApi.ApprovalTaskResponse> myTasks() {
        String currentActor = actor();
        Set<String> userRoles = userRoles();

        List<ApprovalInstance> activeInstances = instanceRepository.findByStatusIn(List.of("SUBMITTED", "UNDER_REVIEW"));
        List<ApprovalApi.ApprovalTaskResponse> tasks = new ArrayList<>();

        for (ApprovalInstance instance : activeInstances) {
            List<ApprovalWorkflowStep> steps = stepRepository.findByWorkflowDefinitionIdOrderByStepOrderAsc(instance.getWorkflowDefinitionId());
            Optional<ApprovalWorkflowStep> currentStepOpt = steps.stream().filter(s -> s.getStepOrder() == instance.getCurrentStepOrder()).findFirst();
            if (currentStepOpt.isEmpty()) continue;
            ApprovalWorkflowStep step = currentStepOpt.get();

            boolean roleMatch = step.getRequiredRole() != null && userRoles.contains(step.getRequiredRole());
            boolean userMatch = step.getRequiredUserId() != null && step.getRequiredUserId().equalsIgnoreCase(currentActor);
            boolean isAdmin = userRoles.contains("SUPER_ADMIN") || userRoles.contains("ADMIN");

            if (roleMatch || userMatch || isAdmin) {
                tasks.add(new ApprovalApi.ApprovalTaskResponse(
                        instance.getId(), instance.getDocumentType(), instance.getDocumentId(),
                        instance.getCurrentStepOrder(), step.getName(), step.getRequiredRole(),
                        instance.getStatus(), instance.getSubmittedBy(), instance.getSubmittedAt().toEpochMilli()
                ));
            }
        }
        return tasks;
    }

    @Transactional(readOnly = true)
    public ApprovalApi.ApprovalInstanceDetailResponse getHistory(String documentType, String documentId) {
        ApprovalInstance instance = instanceRepository.findByDocumentTypeAndDocumentId(documentType.strip().toUpperCase(), documentId)
                .orElseThrow(() -> new BusinessRuleException("لم يتم العثور على طلب اعتماد للمستند المحدد.", "APPROVAL_INSTANCE_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<ApprovalApi.DecisionResponse> decisions = decisionRepository.findByInstanceIdOrderByDecidedAtAsc(instance.getId()).stream()
                .map(d -> new ApprovalApi.DecisionResponse(
                        d.getId(), d.getInstanceId(), d.getStepId(), d.getDecision(),
                        d.getComment(), d.getDecidedBy(), d.getDecidedAt().toEpochMilli()
                )).toList();

        return new ApprovalApi.ApprovalInstanceDetailResponse(
                instance.getId(), instance.getDocumentType(), instance.getDocumentId(),
                instance.getCurrentStepOrder(), instance.getStatus(), instance.getSubmittedBy(),
                instance.getSubmittedAt().toEpochMilli(),
                instance.getCompletedAt() != null ? instance.getCompletedAt().toEpochMilli() : null,
                decisions
        );
    }

    private void saveSteps(String definitionId, List<ApprovalApi.StepRequest> steps) {
        if (steps == null) return;
        for (ApprovalApi.StepRequest s : steps) {
            ApprovalWorkflowStep step = new ApprovalWorkflowStep(
                    definitionId, s.stepOrder(), s.stepCode(), s.name(),
                    s.requiredRole(), s.requiredUserId(), s.amountFrom(), s.amountTo(),
                    s.minimumApprovals(), s.allowSelfApproval(), s.escalationHours()
            );
            stepRepository.save(step);
        }
    }

    private ApprovalInstance requireActiveInstance(String instanceId) {
        ApprovalInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessRuleException("طلب الاعتماد غير موجود: " + instanceId, "APPROVAL_INSTANCE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if ("APPROVED".equals(instance.getStatus()) || "REJECTED".equals(instance.getStatus())) {
            throw new BusinessRuleException("تم حسم طلب الاعتماد سابقاً ولا يمكن تعديله.", "APPROVAL_CLOSED", HttpStatus.CONFLICT);
        }
        return instance;
    }

    private ApprovalWorkflowStep requireCurrentStep(ApprovalInstance instance) {
        return stepRepository.findByWorkflowDefinitionIdAndStepOrder(instance.getWorkflowDefinitionId(), instance.getCurrentStepOrder())
                .orElseThrow(() -> new BusinessRuleException("الخطوة الحالية في مسار الاعتماد غير ملقاة.", "APPROVAL_STEP_NOT_FOUND", HttpStatus.CONFLICT));
    }

    private void validateUserAuthorization(ApprovalWorkflowStep step, String username) {
        Set<String> roles = userRoles();
        if (roles.contains("SUPER_ADMIN") || roles.contains("ADMIN")) return;

        boolean roleMatch = step.getRequiredRole() != null && roles.contains(step.getRequiredRole());
        boolean userMatch = step.getRequiredUserId() != null && step.getRequiredUserId().equalsIgnoreCase(username);

        if (!roleMatch && !userMatch) {
            throw new BusinessRuleException("ليس لديك صلاحية اعتماد خطوة المستند الحالية.", "APPROVAL_NOT_AUTHORIZED", HttpStatus.FORBIDDEN);
        }
    }

    private Set<String> userRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
    }

    private String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private ApprovalApi.WorkflowDefinitionResponse mapDefinitionToResponse(ApprovalWorkflowDefinition def) {
        List<ApprovalApi.StepResponse> steps = stepRepository.findByWorkflowDefinitionIdOrderByStepOrderAsc(def.getId()).stream()
                .map(s -> new ApprovalApi.StepResponse(
                        s.getId(), s.getStepOrder(), s.getStepCode(), s.getName(),
                        s.getRequiredRole(), s.getRequiredUserId(), s.getAmountFrom(), s.getAmountTo(),
                        s.getMinimumApprovals(), s.isAllowSelfApproval(), s.getEscalationHours()
                )).toList();

        return new ApprovalApi.WorkflowDefinitionResponse(
                def.getId(), def.getDocumentType(), def.getName(), def.isActive(),
                def.getVersion(), steps, def.getCreatedAt().toEpochMilli(), def.getUpdatedAt().toEpochMilli()
        );
    }
}
