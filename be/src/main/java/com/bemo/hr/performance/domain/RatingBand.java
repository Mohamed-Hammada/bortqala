package com.bemo.hr.performance.domain;

import java.math.BigDecimal;

public enum RatingBand {
    OUTSTANDING,
    EXCEEDS_EXPECTATIONS,
    MEETS_EXPECTATIONS,
    NEEDS_IMPROVEMENT,
    UNSATISFACTORY;

    public static RatingBand fromScore(BigDecimal score) {
        if (score == null) return MEETS_EXPECTATIONS;
        double val = score.doubleValue();
        if (val >= 90.0) return OUTSTANDING;
        if (val >= 80.0) return EXCEEDS_EXPECTATIONS;
        if (val >= 65.0) return MEETS_EXPECTATIONS;
        if (val >= 50.0) return NEEDS_IMPROVEMENT;
        return UNSATISFACTORY;
    }
}
