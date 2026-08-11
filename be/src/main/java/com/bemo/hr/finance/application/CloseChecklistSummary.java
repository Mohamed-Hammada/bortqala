package com.bemo.hr.finance.application;

import java.util.List;

public record CloseChecklistSummary(
    String periodId,
    String periodName,
    boolean canClose,
    List<CloseCheckItem> checks
) {}
