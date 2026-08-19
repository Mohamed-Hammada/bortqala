package com.bemo.hr.project.domain;

public enum AdjustmentType {
    RETENTION,           // Workmanship retention / guarantee deduction (e.g. 5% or 10%)
    ADVANCE_RECOVERY,    // Mobilization advance payment recovery amortization
    VAT_TAX,             // Value Added Tax addition
    WITHHOLDING_TAX,     // Commercial & industrial tax deduction
    PENALTY_DEDUCTION,   // Delay penalty or safety non-compliance deduction
    OTHER_DEDUCTION      // Miscellaneous site deduction
}
