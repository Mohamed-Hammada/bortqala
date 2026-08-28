package com.bemo.hr.compliance.privacy.application;

import com.bemo.hr.compliance.privacy.domain.ConsentRegistry;
import com.bemo.hr.compliance.privacy.domain.ConsentRegistryRepository;
import com.bemo.hr.compliance.privacy.domain.PrivacyRequest;
import com.bemo.hr.compliance.privacy.domain.PrivacyRequestRepository;
import com.bemo.hr.compliance.privacy.domain.RetentionPolicy;
import com.bemo.hr.compliance.privacy.domain.RetentionPolicyRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PrivacyService {

    private final PrivacyRequestRepository privacyRequestRepository;
    private final ConsentRegistryRepository consentRegistryRepository;
    private final RetentionPolicyRepository retentionPolicyRepository;
    private final com.bemo.hr.employee.infrastructure.EmployeeRepository employeeRepository;
    private final com.bemo.hr.party.BusinessPartyRepository businessPartyRepository;
    private final com.bemo.hr.audit.application.AuditService auditService;

    public PrivacyRequest createRequest(PrivacyApi.CreateRequest request) {
        String appId = TenantContext.require();
        PrivacyRequest pr = new PrivacyRequest(
                appId,
                PrivacyRequest.SubjectType.valueOf(request.subjectType()),
                request.subjectRef(),
                PrivacyRequest.Kind.valueOf(request.kind())
        );
        PrivacyRequest saved = privacyRequestRepository.save(pr);
        auditService.record("PRIVACY_CREATE_REQUEST", "PRIVACY_REQUEST", saved.getId(),
                currentActor(), "{\"kind\":\"" + request.kind() + "\",\"subjectType\":\""
                        + request.subjectType() + "\",\"subjectRef\":\"" + request.subjectRef() + "\"}", null);
        return saved;
    }

    public PrivacyRequest decideRequest(String id, PrivacyApi.DecideRequest request, String actor) {
        PrivacyRequest pr = findByIdOrThrow(id);
        if ("COMPLETED".equals(request.decision())) {
            pr.markCompleted(actor, null);
        } else if ("REJECTED".equals(request.decision())) {
            if (request.legalNote() == null || request.legalNote().isBlank()) {
                throw new BusinessRuleException("Legal note is required for rejection",
                        "PRIVACY_REJECTION_NOTE_REQUIRED", HttpStatus.BAD_REQUEST);
            }
            pr.reject(actor, request.legalNote());
        } else {
            throw new BusinessRuleException("Invalid decision", "PRIVACY_INVALID_DECISION", HttpStatus.BAD_REQUEST);
        }
        PrivacyRequest saved = privacyRequestRepository.save(pr);
        auditService.record("PRIVACY_DECIDE_REQUEST", "PRIVACY_REQUEST", saved.getId(), actor,
                "{\"decision\":\"" + request.decision() + "\"}", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PrivacyRequest> listRequests() {
        return privacyRequestRepository.findByAppIdOrderByCreatedAtDesc(TenantContext.require());
    }

    @Transactional(readOnly = true)
    public PrivacyRequest getRequest(String id) {
        return findByIdOrThrow(id);
    }

    public String exportSubjectData(String subjectType, String subjectRef) {
        String appId = TenantContext.require();
        StringBuilder json = new StringBuilder();
        json.append("{\"subjectType\":\"").append(escape(subjectType))
                .append("\",\"subjectRef\":\"").append(escape(subjectRef)).append("\",\"pii\":{");

        boolean wroteField = false;
        if ("EMPLOYEE".equals(subjectType)) {
            var employee = employeeRepository.findByEmployeeCodeIgnoreCase(subjectRef);
            if (employee.isPresent()) {
                var e = employee.get();
                json.append("\"employee\":{\"employeeCode\":\"").append(escape(e.getEmployeeCode()))
                        .append("\",\"fullName\":\"").append(escape(e.getFullName()))
                        .append("\",\"deviceUserId\":\"").append(escape(e.getDeviceUserId()))
                        .append("\",\"categoryId\":\"").append(escape(e.getCategoryId())).append("\"}");
                wroteField = true;
            }
        } else if ("PARTY".equals(subjectType)) {
            var party = businessPartyRepository.findByCodeIgnoreCase(subjectRef);
            if (party.isPresent()) {
                var p = party.get();
                json.append("\"party\":{\"code\":\"").append(escape(p.getCode()))
                        .append("\",\"name\":\"").append(escape(p.getName()))
                        .append("\",\"nameEn\":\"").append(escape(p.getNameEn()))
                        .append("\",\"phone\":\"").append(escape(p.getPhone()))
                        .append("\",\"email\":\"").append(escape(p.getEmail()))
                        .append("\",\"address\":\"").append(escape(p.getAddress()))
                        .append("\",\"taxId\":\"").append(escape(p.getTaxId())).append("\"}");
                wroteField = true;
            }
        }
        if (!wroteField) {
            json.append("\"none\":true");
        }

        json.append("},\"attachments\":[],\"dataController\":\"Bemo ERP\"")
                .append(",\"exportedAt\":\"").append(Instant.now()).append("\"}");
        auditService.record("PRIVACY_EXPORT", "PRIVACY_REQUEST", subjectRef, currentActor(),
                "{\"subjectType\":\"" + subjectType + "\",\"subjectRef\":\"" + subjectRef + "\"}", null);
        return json.toString();
    }

    public List<String> eraseSubjectData(String subjectType, String subjectRef, String actor) {
        String appId = TenantContext.require();
        List<String> retainedTypes = new ArrayList<>();

        if ("EMPLOYEE".equals(subjectType)) {
            var employee = employeeRepository.findByEmployeeCodeIgnoreCase(subjectRef);
            if (employee.isPresent()) {
                var e = employee.get();
                e.update(e.getEmployeeCode(), "<anonymized>", e.getDeviceUserId(), e.getCategoryId(),
                        e.getEmploymentType(), e.getBaseSalary(), e.getActiveFrom(), e.getActiveTo(), e.isActive());
                employeeRepository.save(e);
            }
            retainedTypes.add("EMPLOYEE_FINANCIAL_RECORDS");
        } else if ("PARTY".equals(subjectType)) {
            retainedTypes.add("PARTY_FINANCIAL_RECORDS");
        }

        log.info("Privacy erase executed for {} {} by {} — retained: {}", subjectType, subjectRef, actor, retainedTypes);
        return retainedTypes;
    }

    public ConsentRegistry grantConsent(PrivacyApi.ConsentRequest request) {
        String appId = TenantContext.require();
        ConsentRegistry consent = new ConsentRegistry(appId, request.subjectRef(), request.subjectType(), request.purposeKey());
        return consentRegistryRepository.save(consent);
    }

    public ConsentRegistry withdrawConsent(PrivacyApi.ConsentWithdrawRequest request) {
        String appId = TenantContext.require();
        var consents = consentRegistryRepository.findByAppIdAndSubjectRefAndWithdrawnAtIsNull(appId, request.subjectRef());
        ConsentRegistry consent = consents.stream()
                .filter(c -> c.getPurposeKey().equals(request.purposeKey()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Active consent not found", "PRIVACY_CONSENT_NOT_FOUND"));
        consent.withdraw();
        ConsentRegistry saved = consentRegistryRepository.save(consent);
        auditService.record("PRIVACY_WITHDRAW_CONSENT", "CONSENT_REGISTRY", saved.getId(), currentActor(),
                "{\"subjectRef\":\"" + request.subjectRef() + "\",\"purposeKey\":\"" + request.purposeKey() + "\"}", null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ConsentRegistry> listConsents(String subjectRef) {
        return consentRegistryRepository.findByAppIdAndSubjectRef(TenantContext.require(), subjectRef);
    }

    public RetentionPolicy createRetentionPolicy(PrivacyApi.RetentionPolicyRequest request) {
        String appId = TenantContext.require();
        RetentionPolicy policy = new RetentionPolicy(appId, request.entityKey(), request.months(),
                RetentionPolicy.Action.valueOf(request.action()));
        return retentionPolicyRepository.save(policy);
    }

    public RetentionPolicy updateRetentionPolicy(String id, PrivacyApi.RetentionPolicyRequest request) {
        RetentionPolicy policy = findPolicyByIdOrThrow(id);
        policy.setMonths(request.months());
        policy.setAction(RetentionPolicy.Action.valueOf(request.action()));
        return retentionPolicyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public List<RetentionPolicy> listRetentionPolicies() {
        return retentionPolicyRepository.findByAppIdAndActiveTrue(TenantContext.require());
    }

    @Transactional(readOnly = true)
    public List<PrivacyApi.DryRunResult> dryRunRetention() {
        String appId = TenantContext.require();
        List<RetentionPolicy> policies = retentionPolicyRepository.findByAppIdAndActiveTrue(appId);
        List<PrivacyApi.DryRunResult> results = new ArrayList<>();
        for (RetentionPolicy policy : policies) {
            long count = 0;
            if ("EMPLOYEE".equals(policy.getEntityKey())) {
                count = employeeRepository.countByAppIdAndCreatedAtBefore(appId, cutoff(policy));
            } else if ("PARTY".equals(policy.getEntityKey())) {
                count = businessPartyRepository.countByAppIdAndCreatedAtBefore(appId, cutoff(policy));
            }
            results.add(new PrivacyApi.DryRunResult(policy.getEntityKey(), count, policy.getAction().name()));
        }
        return results;
    }

    private Instant cutoff(RetentionPolicy policy) {
        return Instant.now().minus(Duration.ofDays(30L * policy.getMonths()));
    }

    private String currentActor() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            return auth.getName();
        }
        return "system";
    }

    private PrivacyRequest findByIdOrThrow(String id) {
        return privacyRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Privacy request not found", "PRIVACY_REQUEST_NOT_FOUND"));
    }

    private RetentionPolicy findPolicyByIdOrThrow(String id) {
        return retentionPolicyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Retention policy not found", "PRIVACY_POLICY_NOT_FOUND"));
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
