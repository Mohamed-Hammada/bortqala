package com.bemo.hr.approval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
public class SegregationOfDutiesService {

    public void validateRequesterNotApprover(String requesterUsername, String approverUsername, boolean allowSelfApproval) {
        log.debug("validateRequesterNotApprover called with requesterUsername={}, approverUsername={}, allowSelfApproval={}", requesterUsername, approverUsername, allowSelfApproval);
        if (!allowSelfApproval && Objects.equals(requesterUsername, approverUsername)) {
            log.warn("Validation failed: self-approval not allowed for {}", approverUsername);
            throw new SegregationOfDutiesViolationException(
                    "SELF_APPROVAL_DISABLED",
                    approverUsername,
                    "The creator of the document cannot approve their own request."
            );
        }
    }

    public void validateCreatorNotPoster(String creatorUsername, String posterUsername, String actionName) {
        log.debug("validateCreatorNotPoster called with creatorUsername={}, posterUsername={}, actionName={}", creatorUsername, posterUsername, actionName);
        if (Objects.equals(creatorUsername, posterUsername)) {
            log.warn("Validation failed: creator-poster conflict for {} action {}", posterUsername, actionName);
            throw new SegregationOfDutiesViolationException(
                    "CREATOR_POSTER_CONFLICT",
                    posterUsername,
                    String.format("The user who created the entry cannot execute %s.", actionName)
            );
        }
    }

    public void validatePreparerNotDisburser(String preparerUsername, String disburserUsername, BigDecimal amount, BigDecimal threshold) {
        log.debug("validatePreparerNotDisburser called with preparerUsername={}, disburserUsername={}, amount={}, threshold={}", preparerUsername, disburserUsername, amount, threshold);
        if (amount != null && threshold != null && amount.compareTo(threshold) >= 0) {
            if (Objects.equals(preparerUsername, disburserUsername)) {
                log.warn("Validation failed: high-value SOD conflict for {} amount {}", disburserUsername, amount);
                throw new SegregationOfDutiesViolationException(
                        "HIGH_VALUE_SOD_CONFLICT",
                        disburserUsername,
                        String.format("Transactions over %s require a different disburser than the preparer.", threshold)
                );
            }
        }
    }

    public void validateClaimCreatorNotCertifier(String creatorUsername, String certifierUsername, String claimType) {
        log.debug("validateClaimCreatorNotCertifier called with creator={}, certifier={}, type={}",
                creatorUsername, certifierUsername, claimType);
        if (Objects.equals(creatorUsername, certifierUsername)) {
            log.warn("Validation failed: claim creator-certifier conflict for {} on {}", certifierUsername, claimType);
            throw new SegregationOfDutiesViolationException(
                    "CLAIM_CREATOR_CERTIFIER_CONFLICT",
                    certifierUsername,
                    String.format("The user who prepared the %s cannot certify or approve it.", claimType)
            );
        }
    }

    public void validateDualAuthorizationRequired(int requiredApprovals, int distinctApproversCount, BigDecimal amount, BigDecimal dualAuthThreshold) {
        log.debug("validateDualAuthorizationRequired called: required={}, distinct={}, amount={}, threshold={}",
                requiredApprovals, distinctApproversCount, amount, dualAuthThreshold);
        if (amount != null && dualAuthThreshold != null && amount.compareTo(dualAuthThreshold) >= 0) {
            if (distinctApproversCount < Math.max(requiredApprovals, 2)) {
                log.warn("Dual authorization check failed: only {} of minimum 2 distinct approvers have signed", distinctApproversCount);
                throw new SegregationOfDutiesViolationException(
                        "DUAL_AUTHORIZATION_REQUIRED",
                        "MULTIPLE_APPROVERS",
                        String.format("Transactions exceeding %s require at least 2 distinct authorized approvers.", dualAuthThreshold)
                );
            }
        }
    }
}
