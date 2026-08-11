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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {
    private static final List<String> ACTIVE_STATUSES = List.of("SUBMITTED", "UNDER_REVIEW");
    private final ApprovalWorkflowDefinitionRepository definitionRepository;
    private final ApprovalWorkflowStepRepository stepRepository;
    private final ApprovalInstanceRepository instanceRepository;
    private final ApprovalInstanceStepRepository instanceStepRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final ApprovalDelegationRepository delegationRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public boolean hasActiveWorkflow(String documentType) {
        return !definitionRepository.findByDocumentTypeAndActiveTrue(normalizeType(documentType)).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<ApprovalApi.WorkflowDefinitionResponse> listWorkflowDefinitions() {
        return definitionRepository.findAll().stream()
                .sorted(Comparator.comparing(ApprovalWorkflowDefinition::getDocumentType))
                .map(this::mapDefinitionToResponse).toList();
    }

    @Transactional(readOnly = true)
    public ApprovalApi.WorkflowDefinitionResponse getWorkflowDefinition(String id) {
        return mapDefinitionToResponse(requireDefinition(id));
    }

    @Transactional
    public ApprovalApi.WorkflowDefinitionResponse createWorkflowDefinition(ApprovalApi.WorkflowDefinitionRequest request) {
        validateSteps(request.steps());
        ApprovalWorkflowDefinition def = definitionRepository.save(
                new ApprovalWorkflowDefinition(request.documentType(), request.name(), request.active()));
        saveSteps(def.getId(), request.steps());
        auditService.record("CREATE", "APPROVAL_WORKFLOW_DEFINITION", def.getId(), actor(),
                "{\"documentType\":\"" + def.getDocumentType() + "\"}", null);
        return mapDefinitionToResponse(def);
    }

    @Transactional
    public ApprovalApi.WorkflowDefinitionResponse updateWorkflowDefinition(String id, ApprovalApi.WorkflowDefinitionRequest request) {
        validateSteps(request.steps());
        ApprovalWorkflowDefinition def = requireDefinition(id);
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
        String type = normalizeType(request.documentType());
        ApprovalWorkflowDefinition def = definitionRepository.findByDocumentTypeAndActiveTrue(type).stream()
                .findFirst().orElseThrow(() -> error("APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.NOT_FOUND));
        List<ApprovalWorkflowStep> applicable = stepRepository
                .findByWorkflowDefinitionIdOrderByStepOrderAsc(def.getId()).stream()
                .filter(step -> appliesToAmount(step, request.amount())).toList();
        if (applicable.isEmpty()) throw error("APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.CONFLICT);

        instanceRepository.findByDocumentTypeAndDocumentId(type, request.documentId()).ifPresent(existing -> {
            throw error("APPROVAL_ALREADY_SUBMITTED", HttpStatus.CONFLICT);
        });
        String snapshot = request.snapshotJson() == null || request.snapshotJson().isBlank() ? "{}" : request.snapshotJson().strip();
        if (snapshot.length() > 4000) throw error("APPROVAL_SNAPSHOT_TOO_LARGE", HttpStatus.BAD_REQUEST);

        ApprovalWorkflowStep first = applicable.get(0);
        ApprovalInstance instance = instanceRepository.save(new ApprovalInstance(
                def.getId(), def.getVersion(), type, request.documentId(), actor(), snapshot, first.getEscalationHours()));
        for (int index = 0; index < applicable.size(); index++) {
            instanceStepRepository.save(new ApprovalInstanceStep(instance.getId(), applicable.get(index), index + 1));
        }
        auditService.record("SUBMIT", "APPROVAL_INSTANCE", instance.getId(), actor(),
                "{\"documentId\":\"" + request.documentId() + "\",\"definitionVersion\":" + def.getVersion() + "}", null);
        return getHistory(type, request.documentId());
    }

    @Transactional
    public ApprovalApi.ApprovalInstanceDetailResponse approve(ApprovalApi.DecisionRequest request) {
        ApprovalInstance instance = requireActiveInstance(request.instanceId());
        ApprovalInstanceStep step = requireCurrentStep(instance);
        String currentActor = actor();
        if (!step.isAllowSelfApproval() && currentActor.equalsIgnoreCase(instance.getSubmittedBy())) {
            throw error("APPROVAL_SELF_APPROVAL_BLOCKED", HttpStatus.CONFLICT);
        }
        String delegatedFrom = validateUserAuthorization(step, instance, currentActor);
        if (decisionRepository.existsByInstanceIdAndStepIdAndDecidedByIgnoreCaseAndDecision(
                instance.getId(), step.getId(), currentActor, "APPROVED")) {
            throw error("APPROVAL_DUPLICATE_DECISION", HttpStatus.CONFLICT);
        }
        decisionRepository.save(new ApprovalDecision(instance.getId(), step.getId(), "APPROVED",
                request.comment(), currentActor, delegatedFrom));
        long approvals = decisionRepository.countByInstanceIdAndStepIdAndDecision(instance.getId(), step.getId(), "APPROVED");
        if (approvals >= step.getMinimumApprovals()) {
            instanceStepRepository.findByInstanceIdAndStepOrder(instance.getId(), instance.getCurrentStepOrder() + 1)
                    .ifPresentOrElse(next -> instance.advanceStep(next.getStepOrder(), next.getEscalationHours()), instance::approve);
            instanceRepository.save(instance);
        }
        auditService.record("APPROVE_STEP", "APPROVAL_INSTANCE", instance.getId(), currentActor,
                "{\"stepOrder\":" + step.getStepOrder() + ",\"approvals\":" + approvals
                        + ",\"required\":" + step.getMinimumApprovals() + "}", delegatedFrom);
        return getHistory(instance.getDocumentType(), instance.getDocumentId());
    }

    @Transactional
    public ApprovalApi.ApprovalInstanceDetailResponse reject(ApprovalApi.DecisionRequest request) {
        if (request.comment() == null || request.comment().isBlank()) {
            throw error("APPROVAL_REJECTION_REASON_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        ApprovalInstance instance = requireActiveInstance(request.instanceId());
        ApprovalInstanceStep step = requireCurrentStep(instance);
        String currentActor = actor();
        String delegatedFrom = validateUserAuthorization(step, instance, currentActor);
        decisionRepository.save(new ApprovalDecision(instance.getId(), step.getId(), "REJECTED",
                request.comment().strip(), currentActor, delegatedFrom));
        instance.reject();
        instanceRepository.save(instance);
        auditService.record("REJECT_STEP", "APPROVAL_INSTANCE", instance.getId(), currentActor,
                "{\"stepOrder\":" + step.getStepOrder() + "}", delegatedFrom);
        return getHistory(instance.getDocumentType(), instance.getDocumentId());
    }

    @Transactional(readOnly = true)
    public List<ApprovalApi.ApprovalTaskResponse> myTasks() {
        String currentActor = actor();
        Set<String> roles = userRoles();
        Instant now = Instant.now();
        List<ApprovalDelegation> delegated = delegationRepository
                .findByDelegateUserIdIgnoreCaseAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(currentActor, now, now);
        List<ApprovalApi.ApprovalTaskResponse> tasks = new ArrayList<>();
        for (ApprovalInstance instance : instanceRepository.findByStatusIn(ACTIVE_STATUSES)) {
            ApprovalInstanceStep step = instanceStepRepository
                    .findByInstanceIdAndStepOrder(instance.getId(), instance.getCurrentStepOrder()).orElse(null);
            if (step == null) continue;
            String delegatedFrom = delegated.stream()
                    .filter(d -> step.getRequiredUserId() != null
                            && d.applies(step.getRequiredUserId(), currentActor, instance.getDocumentType(), now))
                    .map(ApprovalDelegation::getDelegatorUserId).findFirst().orElse(null);
            boolean authorized = isAdmin(roles)
                    || step.getRequiredRole() != null && roles.contains(step.getRequiredRole())
                    || step.getRequiredUserId() != null && step.getRequiredUserId().equalsIgnoreCase(currentActor)
                    || delegatedFrom != null;
            if (authorized) {
                long received = decisionRepository.countByInstanceIdAndStepIdAndDecision(instance.getId(), step.getId(), "APPROVED");
                tasks.add(new ApprovalApi.ApprovalTaskResponse(instance.getId(), instance.getDocumentType(), instance.getDocumentId(),
                        instance.getCurrentStepOrder(), step.getName(), step.getRequiredRole(), instance.getStatus(),
                        instance.getSubmittedBy(), instance.getSubmittedAt().toEpochMilli(), epoch(instance.getStepDueAt()),
                        instance.isOverdue(now), instance.getEscalationLevel(), Math.toIntExact(received),
                        step.getMinimumApprovals(), delegatedFrom));
            }
        }
        return tasks;
    }

    @Transactional(readOnly = true)
    public ApprovalApi.ApprovalInstanceDetailResponse getHistory(String documentType, String documentId) {
        ApprovalInstance instance = instanceRepository.findByDocumentTypeAndDocumentId(normalizeType(documentType), documentId)
                .orElseThrow(() -> error("APPROVAL_INSTANCE_NOT_FOUND", HttpStatus.NOT_FOUND));
        ApprovalInstanceStep current = instanceStepRepository
                .findByInstanceIdAndStepOrder(instance.getId(), instance.getCurrentStepOrder()).orElse(null);
        int required = current == null ? 0 : current.getMinimumApprovals();
        int received = current == null ? 0 : Math.toIntExact(decisionRepository
                .countByInstanceIdAndStepIdAndDecision(instance.getId(), current.getId(), "APPROVED"));
        List<ApprovalApi.DecisionResponse> decisions = decisionRepository.findByInstanceIdOrderByDecidedAtAsc(instance.getId()).stream()
                .map(d -> new ApprovalApi.DecisionResponse(d.getId(), d.getInstanceId(), d.getStepId(), d.getDecision(),
                        d.getComment(), d.getDecidedBy(), d.getDecidedAt().toEpochMilli(), d.getDelegatedFrom())).toList();
        return new ApprovalApi.ApprovalInstanceDetailResponse(instance.getId(), instance.getDocumentType(), instance.getDocumentId(),
                instance.getCurrentStepOrder(), instance.getStatus(), instance.getSubmittedBy(), instance.getSubmittedAt().toEpochMilli(),
                epoch(instance.getCompletedAt()), instance.getWorkflowDefinitionVersion(), instance.getDocumentSnapshotJson(),
                epoch(instance.getStepDueAt()), instance.isOverdue(Instant.now()), instance.getEscalationLevel(), received, required, decisions);
    }

    @Transactional(readOnly = true)
    public List<ApprovalApi.DelegationResponse> listDelegations() {
        String currentActor = actor();
        boolean admin = isAdmin(userRoles());
        return delegationRepository.findAllByOrderByStartsAtDesc().stream()
                .filter(d -> admin || d.getDelegatorUserId().equalsIgnoreCase(currentActor)
                        || d.getDelegateUserId().equalsIgnoreCase(currentActor))
                .map(this::mapDelegation).toList();
    }

    @Transactional
    public ApprovalApi.DelegationResponse createDelegation(ApprovalApi.DelegationRequest request) {
        String currentActor = actor();
        if (!isAdmin(userRoles()) && !currentActor.equalsIgnoreCase(request.delegatorUserId())) {
            throw error("APPROVAL_NOT_AUTHORIZED", HttpStatus.FORBIDDEN);
        }
        if (request.delegatorUserId().equalsIgnoreCase(request.delegateUserId())) {
            throw error("APPROVAL_DELEGATION_SELF", HttpStatus.BAD_REQUEST);
        }
        Instant startsAt = Instant.ofEpochMilli(request.startsAt());
        Instant endsAt = Instant.ofEpochMilli(request.endsAt());
        if (!endsAt.isAfter(startsAt)) throw error("APPROVAL_DELEGATION_DATES_INVALID", HttpStatus.BAD_REQUEST);
        ApprovalDelegation saved = delegationRepository.save(new ApprovalDelegation(request.delegatorUserId(),
                request.delegateUserId(), request.documentType(), startsAt, endsAt, request.reason(), currentActor));
        auditService.record("CREATE", "APPROVAL_DELEGATION", saved.getId(), currentActor, null, null);
        return mapDelegation(saved);
    }

    @Transactional
    public void deactivateDelegation(String id) {
        ApprovalDelegation delegation = delegationRepository.findById(id)
                .orElseThrow(() -> error("APPROVAL_DELEGATION_NOT_FOUND", HttpStatus.NOT_FOUND));
        String currentActor = actor();
        if (!isAdmin(userRoles()) && !currentActor.equalsIgnoreCase(delegation.getDelegatorUserId())) {
            throw error("APPROVAL_NOT_AUTHORIZED", HttpStatus.FORBIDDEN);
        }
        delegation.deactivate();
        delegationRepository.save(delegation);
        auditService.record("DEACTIVATE", "APPROVAL_DELEGATION", id, currentActor, null, null);
    }

    @Transactional
    public ApprovalApi.ApprovalInstanceDetailResponse reassign(String instanceId, ApprovalApi.ReassignRequest request) {
        ApprovalInstance instance = requireActiveInstance(instanceId);
        ApprovalInstanceStep step = requireCurrentStep(instance);
        step.reassign(request.userId(), actor(), request.reason());
        instanceStepRepository.save(step);
        auditService.record("REASSIGN", "APPROVAL_INSTANCE", instanceId, actor(),
                "{\"userId\":\"" + request.userId().strip() + "\"}", request.reason().strip());
        return getHistory(instance.getDocumentType(), instance.getDocumentId());
    }

    @Transactional
    public int escalateOverdue() {
        Instant now = Instant.now();
        List<ApprovalInstance> overdue = instanceRepository
                .findByStatusInAndStepDueAtBeforeAndEscalatedAtIsNull(ACTIVE_STATUSES, now);
        overdue.forEach(instance -> {
            instance.escalate(now);
            instanceRepository.save(instance);
            auditService.record("ESCALATE", "APPROVAL_INSTANCE", instance.getId(), actor(),
                    "{\"level\":" + instance.getEscalationLevel() + "}", null);
        });
        return overdue.size();
    }

    private void validateSteps(List<ApprovalApi.StepRequest> steps) {
        if (steps == null || steps.isEmpty()) throw error("APPROVAL_STEPS_REQUIRED", HttpStatus.BAD_REQUEST);
        List<ApprovalApi.StepRequest> sorted = steps.stream().sorted(Comparator.comparingInt(ApprovalApi.StepRequest::stepOrder)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            ApprovalApi.StepRequest step = sorted.get(i);
            if (step.stepOrder() != i + 1 || step.minimumApprovals() < 1
                    || step.requiredRole() == null && step.requiredUserId() == null
                    || step.decisionPolicy() != null && !"ANY_N".equalsIgnoreCase(step.decisionPolicy())) {
                throw error("APPROVAL_STEP_INVALID", HttpStatus.BAD_REQUEST);
            }
            if (step.amountFrom() != null && step.amountTo() != null && step.amountFrom().compareTo(step.amountTo()) > 0) {
                throw error("APPROVAL_STEP_INVALID", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void saveSteps(String definitionId, List<ApprovalApi.StepRequest> steps) {
        steps.stream().sorted(Comparator.comparingInt(ApprovalApi.StepRequest::stepOrder)).forEach(s ->
                stepRepository.save(new ApprovalWorkflowStep(definitionId, s.stepOrder(), s.stepCode(), s.name(),
                        s.requiredRole(), s.requiredUserId(), s.amountFrom(), s.amountTo(), s.minimumApprovals(),
                        s.allowSelfApproval(), s.escalationHours(), s.decisionPolicy())));
    }

    private ApprovalInstance requireActiveInstance(String id) {
        ApprovalInstance instance = instanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> error("APPROVAL_INSTANCE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!ACTIVE_STATUSES.contains(instance.getStatus())) throw error("APPROVAL_CLOSED", HttpStatus.CONFLICT);
        return instance;
    }

    private ApprovalInstanceStep requireCurrentStep(ApprovalInstance instance) {
        return instanceStepRepository.findByInstanceIdAndStepOrder(instance.getId(), instance.getCurrentStepOrder())
                .orElseThrow(() -> error("APPROVAL_STEP_NOT_FOUND", HttpStatus.CONFLICT));
    }

    private String validateUserAuthorization(ApprovalInstanceStep step, ApprovalInstance instance, String username) {
        Set<String> roles = userRoles();
        if (isAdmin(roles) || step.getRequiredRole() != null && roles.contains(step.getRequiredRole())
                || step.getRequiredUserId() != null && step.getRequiredUserId().equalsIgnoreCase(username)) return null;
        if (step.getRequiredUserId() != null) {
            Instant now = Instant.now();
            return delegationRepository
                    .findByDelegateUserIdIgnoreCaseAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(username, now, now)
                    .stream().filter(d -> d.applies(step.getRequiredUserId(), username, instance.getDocumentType(), now))
                    .map(ApprovalDelegation::getDelegatorUserId).findFirst()
                    .orElseThrow(() -> error("APPROVAL_NOT_AUTHORIZED", HttpStatus.FORBIDDEN));
        }
        throw error("APPROVAL_NOT_AUTHORIZED", HttpStatus.FORBIDDEN);
    }

    private ApprovalWorkflowDefinition requireDefinition(String id) {
        return definitionRepository.findById(id).orElseThrow(() -> error("APPROVAL_WORKFLOW_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private ApprovalApi.WorkflowDefinitionResponse mapDefinitionToResponse(ApprovalWorkflowDefinition def) {
        List<ApprovalApi.StepResponse> steps = stepRepository.findByWorkflowDefinitionIdOrderByStepOrderAsc(def.getId()).stream()
                .map(s -> new ApprovalApi.StepResponse(s.getId(), s.getStepOrder(), s.getStepCode(), s.getName(),
                        s.getRequiredRole(), s.getRequiredUserId(), s.getAmountFrom(), s.getAmountTo(),
                        s.getMinimumApprovals(), s.isAllowSelfApproval(), s.getEscalationHours(), s.getDecisionPolicy())).toList();
        return new ApprovalApi.WorkflowDefinitionResponse(def.getId(), def.getDocumentType(), def.getName(), def.isActive(),
                def.getVersion(), steps, def.getCreatedAt().toEpochMilli(), def.getUpdatedAt().toEpochMilli());
    }

    private ApprovalApi.DelegationResponse mapDelegation(ApprovalDelegation d) {
        return new ApprovalApi.DelegationResponse(d.getId(), d.getDelegatorUserId(), d.getDelegateUserId(), d.getDocumentType(),
                d.getStartsAt().toEpochMilli(), d.getEndsAt().toEpochMilli(), d.getReason(), d.isActive(),
                d.getCreatedBy(), d.getCreatedAt().toEpochMilli(), d.getVersion());
    }

    private static boolean appliesToAmount(ApprovalWorkflowStep step, BigDecimal amount) {
        if (amount == null) return step.getAmountFrom() == null && step.getAmountTo() == null;
        return (step.getAmountFrom() == null || amount.compareTo(step.getAmountFrom()) >= 0)
                && (step.getAmountTo() == null || amount.compareTo(step.getAmountTo()) <= 0);
    }
    private static String normalizeType(String value) { return value.strip().toUpperCase(); }
    private static Long epoch(Instant value) { return value == null ? null : value.toEpochMilli(); }
    private static boolean isAdmin(Set<String> roles) { return roles.contains("SUPER_ADMIN") || roles.contains("ADMIN"); }
    private static BusinessRuleException error(String code, HttpStatus status) { return new BusinessRuleException(code, code, status); }
    private Set<String> userRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a).collect(Collectors.toSet());
    }
    private String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}
