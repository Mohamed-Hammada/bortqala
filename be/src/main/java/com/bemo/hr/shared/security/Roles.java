package com.bemo.hr.shared.security;

/**
 * Central constants for the role predicates used in {@code @PreAuthorize} annotations.
 *
 * <p>Endpoints compose these building blocks into a SpEL predicate, e.g.
 * {@code @PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.FINANCE_TEAM)}. This keeps the
 * exact role membership of every endpoint explicit while avoiding the previous
 * explosion of duplicated, drift-prone {@code ADMIN_*} role-list constants.
 */
public final class Roles {

    private Roles() {
    }

    // ---- Admin predicates ----

    /** {@code SUPER_ADMIN} or {@code ADMIN}. */
    public static final String ADMIN_ONLY = "hasAnyRole('SUPER_ADMIN','ADMIN')";

    /** {@code SUPER_ADMIN} only. */
    public static final String SUPER_ADMIN_ONLY = "hasRole('SUPER_ADMIN')";

    /** Every operational role (smart-import style endpoints). */
    public static final String ALL_STAFF = "hasAnyRole('SUPER_ADMIN','ADMIN','ACCOUNTANT','AUDITOR','FINANCE_MANAGER','HR_MANAGER','HR_REVIEWER','INVENTORY_MANAGER','MANUFACTURING_MANAGER','PAYROLL_MANAGER','PROCUREMENT_MANAGER','PROCUREMENT_USER','QUALITY_MANAGER','SALES_MANAGER','TREASURY_USER')";

    // ---- Functional teams ----

    /** Finance Team. */
    public static final String FINANCE_TEAM = "hasAnyRole('ACCOUNTANT','AUDITOR','FINANCE_MANAGER','TREASURY_USER')";

    /** Hr Team. */
    public static final String HR_TEAM = "hasAnyRole('HR_MANAGER','HR_REVIEWER')";

    /** Procurement Team. */
    public static final String PROCUREMENT_TEAM = "hasAnyRole('PROCUREMENT_MANAGER','PROCUREMENT_USER')";

    /** Workforce Team. */
    public static final String WORKFORCE_TEAM = "hasAnyRole('WORKFORCE_FINANCE','WORKFORCE_MANAGER','WORKFORCE_REVIEWER')";

    // ---- Individual functional roles ----

    /** The {@code FINANCE_MANAGER} role. */
    public static final String FINANCE_MANAGER = "hasAnyRole('FINANCE_MANAGER')";

    /** The {@code ACCOUNTANT} role. */
    public static final String ACCOUNTANT = "hasAnyRole('ACCOUNTANT')";

    /** The {@code AUDITOR} role. */
    public static final String AUDITOR = "hasAnyRole('AUDITOR')";

    /** The {@code TREASURY_USER} role. */
    public static final String TREASURY_USER = "hasAnyRole('TREASURY_USER')";

    /** The {@code TREASURY_MANAGER} role. */
    public static final String TREASURY_MANAGER = "hasAnyRole('TREASURY_MANAGER')";

    /** The {@code HR_MANAGER} role. */
    public static final String HR_MANAGER = "hasAnyRole('HR_MANAGER')";

    /** The {@code HR_REVIEWER} role. */
    public static final String HR_REVIEWER = "hasAnyRole('HR_REVIEWER')";

    /** The {@code HR_OFFICER} role. */
    public static final String HR_OFFICER = "hasAnyRole('HR_OFFICER')";

    /** The {@code EMPLOYEE} role. */
    public static final String EMPLOYEE = "hasAnyRole('EMPLOYEE')";

    /** The {@code PAYROLL_MANAGER} role. */
    public static final String PAYROLL_MANAGER = "hasAnyRole('PAYROLL_MANAGER')";

    /** The {@code WORKFORCE_MANAGER} role. */
    public static final String WORKFORCE_MANAGER = "hasAnyRole('WORKFORCE_MANAGER')";

    /** The {@code WORKFORCE_REVIEWER} role. */
    public static final String WORKFORCE_REVIEWER = "hasAnyRole('WORKFORCE_REVIEWER')";

    /** The {@code WORKFORCE_FINANCE} role. */
    public static final String WORKFORCE_FINANCE = "hasAnyRole('WORKFORCE_FINANCE')";

    /** The {@code PROCUREMENT_MANAGER} role. */
    public static final String PROCUREMENT_MANAGER = "hasAnyRole('PROCUREMENT_MANAGER')";

    /** The {@code PROCUREMENT_USER} role. */
    public static final String PROCUREMENT_USER = "hasAnyRole('PROCUREMENT_USER')";

    /** The {@code INVENTORY_MANAGER} role. */
    public static final String INVENTORY_MANAGER = "hasAnyRole('INVENTORY_MANAGER')";

    /** The {@code MANUFACTURING_MANAGER} role. */
    public static final String MANUFACTURING_MANAGER = "hasAnyRole('MANUFACTURING_MANAGER')";

    /** The {@code QUALITY_MANAGER} role. */
    public static final String QUALITY_MANAGER = "hasAnyRole('QUALITY_MANAGER')";

    /** The {@code SALES_MANAGER} role. */
    public static final String SALES_MANAGER = "hasAnyRole('SALES_MANAGER')";

    /** The {@code PROJECT_MANAGER} role. */
    public static final String PROJECT_MANAGER = "hasAnyRole('PROJECT_MANAGER')";

    /** The {@code VIEWER} role. */
    public static final String VIEWER = "hasAnyRole('VIEWER')";

}
