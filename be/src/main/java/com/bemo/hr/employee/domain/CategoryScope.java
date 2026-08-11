package com.bemo.hr.employee.domain;

/**
 * Visibility scope of a canonical attendance/workforce category.
 * A single canonical category record (attendance_categories) carries the
 * shared identity; this enum decides which contexts may consume it.
 */
public enum CategoryScope {
    EMPLOYEE,
    WORKER,
    BOTH
}
