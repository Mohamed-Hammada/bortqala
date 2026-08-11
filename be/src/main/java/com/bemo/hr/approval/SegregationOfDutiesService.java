package com.bemo.hr.approval;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class SegregationOfDutiesService {

    public void validateRequesterNotApprover(String requesterUsername, String approverUsername, boolean allowSelfApproval) {
        if (!allowSelfApproval && Objects.equals(requesterUsername, approverUsername)) {
            throw new SegregationOfDutiesViolationException(
                    "SELF_APPROVAL_DISABLED",
                    approverUsername,
                    "The creator of the document cannot approve their own request."
            );
        }
    }

    public void validateCreatorNotPoster(String creatorUsername, String posterUsername, String actionName) {
        if (Objects.equals(creatorUsername, posterUsername)) {
            throw new SegregationOfDutiesViolationException(
                    "CREATOR_POSTER_CONFLICT",
                    posterUsername,
                    String.format("The user who created the entry cannot execute %s.", actionName)
            );
        }
    }

    public void validatePreparerNotDisburser(String preparerUsername, String disburserUsername, BigDecimal amount, BigDecimal threshold) {
        if (amount != null && threshold != null && amount.compareTo(threshold) >= 0) {
            if (Objects.equals(preparerUsername, disburserUsername)) {
                throw new SegregationOfDutiesViolationException(
                        "HIGH_VALUE_SOD_CONFLICT",
                        disburserUsername,
                        String.format("Transactions over %s require a different disburser than the preparer.", threshold)
                );
            }
        }
    }
}
