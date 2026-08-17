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
}
