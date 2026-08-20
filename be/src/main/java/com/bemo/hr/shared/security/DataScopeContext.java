package com.bemo.hr.shared.security;

/**
 * Thread-local holder for the authenticated user's data scoping context.
 * Used by Hibernate filters to automatically restrict row-level data access.
 *
 * Scoping levels:
 * - GLOBAL: No filter applied (user sees everything in the tenant)
 * - BRANCH: Filter on branch_id = user's assigned branch
 * - DEPARTMENT: Filter on department_id = user's assigned department
 * - SELF: Filter on created_by = current user ID
 */
public final class DataScopeContext {

    public enum ScopeLevel {
        GLOBAL, BRANCH, DEPARTMENT, SELF
    }

    private static final ThreadLocal<ScopeLevel> CURRENT_SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<String> BRANCH_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> DEPARTMENT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(ScopeLevel scope, String branchId, String departmentId, String userId) {
        CURRENT_SCOPE.set(scope);
        BRANCH_ID.set(branchId);
        DEPARTMENT_ID.set(departmentId);
        USER_ID.set(userId);
    }

    public static ScopeLevel getScope() {
        return CURRENT_SCOPE.get();
    }

    public static String getBranchId() {
        return BRANCH_ID.get();
    }

    public static String getDepartmentId() {
        return DEPARTMENT_ID.get();
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static boolean isGlobal() {
        ScopeLevel scope = CURRENT_SCOPE.get();
        return scope == null || scope == ScopeLevel.GLOBAL;
    }

    public static void clear() {
        CURRENT_SCOPE.remove();
        BRANCH_ID.remove();
        DEPARTMENT_ID.remove();
        USER_ID.remove();
    }
}
