package com.bemo.hr.shared.transition;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class IllegalDocumentTransitionException extends BusinessRuleException {
    public IllegalDocumentTransitionException(String documentType, String documentId, String currentState, String targetState) {
        super(String.format("Cannot transition %s '%s' from state %s to %s", documentType, documentId, currentState, targetState),
                "ILLEGAL_DOCUMENT_TRANSITION",
                HttpStatus.CONFLICT);
    }
}
