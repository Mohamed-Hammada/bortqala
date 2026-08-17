package com.bemo.hr.finance.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CloseCheckItem(
        String code,
        String module,
        Severity severity,
        long count,
        BigDecimal amount,
        String message,
        BigDecimal glBalance,
        BigDecimal subledgerBalance,
        BigDecimal tolerance,
        LocalDate asOfDate,
        String reportReference
) {
    public CloseCheckItem(String code, String module, Severity severity, long count,
                          BigDecimal amount, String message) {
        this(code, module, severity, count, amount, message, null, null, null, null, null);
    }

    public enum Severity {
        PASS, WARNING, BLOCKER
    }
}
