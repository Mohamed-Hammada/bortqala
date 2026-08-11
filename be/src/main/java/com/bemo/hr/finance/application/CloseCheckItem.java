package com.bemo.hr.finance.application;

import java.math.BigDecimal;

public record CloseCheckItem(
    String code,
    String module,
    Severity severity,
    long count,
    BigDecimal amount,
    String message
) {
    public enum Severity {
        PASS, WARNING, BLOCKER
    }
}
