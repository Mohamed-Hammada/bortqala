package com.bemo.hr.project.domain;

public enum TaskConstraintType {
    ASAP,                   // As Soon As Possible
    ALAP,                   // As Late As Possible
    MUST_START_ON,          // Must Start On (Fixed Date)
    MUST_FINISH_ON,         // Must Finish On (Fixed Date)
    START_NO_EARLIER_THAN,  // Start No Earlier Than
    FINISH_NO_LATER_THAN    // Finish No Later Than
}
