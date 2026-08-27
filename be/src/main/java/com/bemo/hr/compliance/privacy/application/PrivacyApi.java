package com.bemo.hr.compliance.privacy.application;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public interface PrivacyApi {

    record CreateRequest(
            @NotBlank String subjectType,
            @NotBlank String subjectRef,
            @NotBlank String kind
    ) {}

    record DecideRequest(
            @NotBlank String decision,
            String legalNote
    ) {}

    record ConsentRequest(
            @NotBlank String subjectRef,
            @NotBlank String subjectType,
            @NotBlank String purposeKey
    ) {}

    record ConsentWithdrawRequest(
            @NotBlank String subjectRef,
            @NotBlank String purposeKey
    ) {}

    record RetentionPolicyRequest(
            @NotBlank String entityKey,
            int months,
            @NotBlank String action
    ) {}

    record Response(
            String id,
            String subjectType,
            String subjectRef,
            String kind,
            String status,
            String legalNote,
            Instant dueAt,
            String decidedBy,
            Instant decidedAt,
            boolean overdue,
            Long version
    ) {
        public static Response from(com.bemo.hr.compliance.privacy.domain.PrivacyRequest r) {
            return new Response(
                    r.getId(), r.getSubjectType().name(), r.getSubjectRef(), r.getKind().name(),
                    r.getStatus().name(), r.getLegalNote(), r.getDueAt(), r.getDecidedBy(),
                    r.getDecidedAt(), r.isOverdue(), r.getVersion()
            );
        }
    }

    record ConsentResponse(
            String id,
            String subjectRef,
            String subjectType,
            String purposeKey,
            Instant grantedAt,
            Instant withdrawnAt,
            boolean active
    ) {
        public static ConsentResponse from(com.bemo.hr.compliance.privacy.domain.ConsentRegistry c) {
            return new ConsentResponse(
                    c.getId(), c.getSubjectRef(), c.getSubjectType(), c.getPurposeKey(),
                    c.getGrantedAt(), c.getWithdrawnAt(), c.isActive()
            );
        }
    }

    record RetentionPolicyResponse(
            String id,
            String entityKey,
            int months,
            String action,
            boolean active,
            Long version
    ) {
        public static RetentionPolicyResponse from(com.bemo.hr.compliance.privacy.domain.RetentionPolicy p) {
            return new RetentionPolicyResponse(
                    p.getId(), p.getEntityKey(), p.getMonths(), p.getAction().name(),
                    p.isActive(), p.getVersion()
            );
        }
    }

    record DryRunResult(String entityKey, long affectedCount, String action) {}
}
