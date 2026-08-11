package com.bemo.hr.approval;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.http.HttpStatus;

public class SegregationOfDutiesViolationException extends BusinessRuleException {
    public SegregationOfDutiesViolationException(String ruleName, String user, String detail) {
        super(String.format("Segregation of duties violation [%s] for user '%s': %s", ruleName, user, detail),
              "SOD_VIOLATION_BLOCKED",
              HttpStatus.FORBIDDEN);
    }
}
