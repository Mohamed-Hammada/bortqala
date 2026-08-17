package com.bemo.hr.shared.transition;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class StaleStateConflictException extends BusinessRuleException {
    public StaleStateConflictException(String documentType, String documentId, long expectedVersion, long actualVersion) {
        super(String.format("%s '%s' was modified by another user (expected version %d, actual version %d). Please refresh and retry.",
                        documentType, documentId, expectedVersion, actualVersion),
                "VERSION_CONFLICT",
                HttpStatus.CONFLICT);
    }
}
