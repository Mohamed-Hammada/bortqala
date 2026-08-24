package com.bemo.hr.access.domain;

import com.bemo.hr.access.domain.AccessDefs.*;
import com.bemo.hr.access.domain.AccessEnums.AccessSensitivity;
import com.bemo.hr.access.domain.AccessEnums.ConflictSeverity;
import com.bemo.hr.access.domain.AccessEnums.RoleKind;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Canonical role-to-page access catalog.
 *
 * <p>This class is the single source of truth for what every backend role can do
 * and which page/action each permission unlocks. It is derived from the
 * {@code @PreAuthorize} role matrices already enforced on the controllers and from
 * the frontend route guards. The catalog is exposed to the UI through
 * {@code GET /api/v1/access/catalog} so the Add/Edit User screens never duplicate
 * authorization mappings; the backend remains authoritative and revalidates every
 * assignment on save.
 */
@Component
public final class AccessCatalog {

    // ------------------------------------------------------------------
    // Permission codes (stable machine keys, never shown as primary text).
    // ------------------------------------------------------------------
    public static final String P_DASHBOARD_VIEW = "dashboard.view";
    public static final String P_EMPLOYEES_READ = "employees.read";
    public static final String P_EMPLOYEES_EDIT = "employees.edit";
    public static final String P_EMPLOYEES_DEACTIVATE = "employees.deactivate";
    public static final String P_CATEGORIES_READ = "categories.read";
    public static final String P_CATEGORIES_MANAGE = "categories.manage";
    public static final String P_IMPORTS_READ = "imports.read";
    public static final String P_IMPORTS_MANAGE = "imports.manage";
    public static final String P_PARTIES_READ = "parties.read";
    public static final String P_PARTIES_MANAGE = "parties.manage";
    public static final String P_REPORTS_READ = "reports.read";
    public static final String P_REPORTS_DECIDE = "reports.decide";
    public static final String P_REPORTS_APPROVE = "reports.approve";
    public static final String P_OPERATIONS_READ = "operations.read";
    public static final String P_OPERATIONS_MANAGE = "operations.manage";
    public static final String P_PROCUREMENT_READ = "procurement.read";
    public static final String P_PROCUREMENT_MANAGE = "procurement.manage";
    public static final String P_SALES_READ = "sales.read";
    public static final String P_SALES_MANAGE = "sales.manage";
    public static final String P_MANUFACTURING_READ = "manufacturing.read";
    public static final String P_MANUFACTURING_MANAGE = "manufacturing.manage";
    public static final String P_QUALITY_READ = "quality.read";
    public static final String P_QUALITY_MANAGE = "quality.manage";
    public static final String P_PAYROLL_READ = "payroll.read";
    public static final String P_PAYROLL_PREPARE = "payroll.prepare";
    public static final String P_PAYROLL_APPROVE = "payroll.approve";
    public static final String P_FINANCE_READ = "finance.read";
    public static final String P_FINANCE_MANAGE = "finance.manage";
    public static final String P_BUDGET_READ = "budget.read";
    public static final String P_BUDGET_MANAGE = "budget.manage";
    public static final String P_ASSET_READ = "asset.read";
    public static final String P_ASSET_MANAGE = "asset.manage";
    public static final String P_JOURNAL_READ = "journal.read";
    public static final String P_JOURNAL_CREATE = "journal.create";
    public static final String P_JOURNAL_POST = "journal.post";
    public static final String P_JOURNAL_REVERSE = "journal.reverse";
    public static final String P_PAYMENTS_EXECUTE = "payments.execute";
    public static final String P_PAYMENTS_APPROVE = "payments.approve";
    public static final String P_INVENTORY_READ = "inventory.read";
    public static final String P_INVENTORY_MANAGE = "inventory.manage";
    public static final String P_ORGANIZATION_READ = "organization.read";
    public static final String P_ORGANIZATION_MANAGE = "organization.manage";
    public static final String P_AUDIT_READ = "audit.read";
    public static final String P_USERS_READ = "users.read";
    public static final String P_USERS_MANAGE = "users.manage";
    public static final String P_ROLES_ASSIGN = "roles.assign";
    public static final String P_SETTINGS_READ = "settings.read";
    public static final String P_SETTINGS_MANAGE = "settings.manage";
    public static final String P_WORKFORCE_DASHBOARD = "workforce.dashboard.read";
    public static final String P_WORKERS_READ = "workers.read";
    public static final String P_WORKERS_CREATE = "workers.create";
    public static final String P_WORKERS_EDIT = "workers.edit";
    public static final String P_WORKERS_DEACTIVATE = "workers.deactivate";
    public static final String P_CONTRACTORS_READ = "contractors.read";
    public static final String P_CONTRACTORS_MANAGE = "contractors.manage";
    public static final String P_LABOR_REQUESTS_READ = "laborRequests.read";
    public static final String P_LABOR_REQUESTS_MANAGE = "laborRequests.manage";
    public static final String P_DISPATCH_DISPUTES_READ = "dispatchDisputes.read";
    public static final String P_DISPATCH_DISPUTES_MANAGE = "dispatchDisputes.manage";
    public static final String P_ATTENDANCE_READ = "attendance.read";
    public static final String P_ATTENDANCE_ENTER = "attendance.enter";
    public static final String P_ATTENDANCE_REVIEW = "attendance.review";
    public static final String P_ATTENDANCE_IMPORT = "attendance.import";
    public static final String P_SETTLEMENTS_READ = "settlements.read";
    public static final String P_SETTLEMENTS_PREPARE = "settlements.prepare";
    public static final String P_SETTLEMENTS_FINALIZE = "settlements.finalize";
    public static final String P_ADVANCES_READ = "advances.read";
    public static final String P_ADVANCES_MANAGE = "advances.manage";
    public static final String P_CONTRACTOR_ACCOUNTS_READ = "contractorAccounts.read";
    public static final String P_CONTRACTOR_ACCOUNTS_MANAGE = "contractorAccounts.manage";
    public static final String P_WORKFORCE_REPORTS_READ = "workforceReports.read";
    public static final String P_APPROVALS_READ = "approvals.read";
    public static final String P_APPROVALS_DECIDE = "approvals.decide";
    public static final String P_WORKFLOW_DEFINITIONS_READ = "workflowDefinitions.read";
    public static final String P_WORKFLOW_DEFINITIONS_MANAGE = "workflowDefinitions.manage";
    public static final String P_PROJECTS_READ = "projects.read";
    public static final String P_PROJECTS_MANAGE = "projects.manage";
    public static final String P_PROJECTS_WBS_MANAGE = "projects.wbs.manage";
    public static final String P_PROJECTS_CLOSE = "projects.close";
    public static final String P_LEAVES_READ = "leaves.read";
    public static final String P_LEAVES_MANAGE = "leaves.manage";
    public static final String P_PERFORMANCE_READ = "performance.read";
    public static final String P_PERFORMANCE_MANAGE = "performance.manage";
    public static final String P_ETA_TAX_READ = "etaTax.read";
    public static final String P_ETA_TAX_MANAGE = "etaTax.manage";
    public static final String P_POS_READ = "pos.read";
    public static final String P_POS_OPERATE = "pos.operate";
    public static final String P_POS_MANAGE = "pos.manage";
    public static final String P_CRM_READ = "crm.read";
    public static final String P_CRM_MANAGE = "crm.manage";
    public static final String P_CRM_OMNICHANNEL = "crm.omnichannel";
    public static final String P_VERTICALS_READ = "verticals.read";
    public static final String P_VERTICALS_MANAGE = "verticals.manage";

    /**
     * Every permission a super user can act on.
     */
    public static final Set<String> ALL_PERMISSIONS = Set.of(
            P_DASHBOARD_VIEW, P_EMPLOYEES_READ, P_EMPLOYEES_EDIT, P_EMPLOYEES_DEACTIVATE,
            P_CATEGORIES_READ, P_CATEGORIES_MANAGE, P_IMPORTS_READ, P_IMPORTS_MANAGE,
            P_PARTIES_READ, P_PARTIES_MANAGE, P_REPORTS_READ, P_REPORTS_DECIDE, P_REPORTS_APPROVE,
            P_OPERATIONS_READ, P_OPERATIONS_MANAGE, P_PROCUREMENT_READ, P_PROCUREMENT_MANAGE,
            P_SALES_READ, P_SALES_MANAGE, P_MANUFACTURING_READ, P_MANUFACTURING_MANAGE,
            P_QUALITY_READ, P_QUALITY_MANAGE, P_PAYROLL_READ, P_PAYROLL_PREPARE, P_PAYROLL_APPROVE,
            P_FINANCE_READ, P_FINANCE_MANAGE, P_JOURNAL_READ, P_JOURNAL_CREATE, P_JOURNAL_POST, P_JOURNAL_REVERSE,
            P_PAYMENTS_EXECUTE, P_PAYMENTS_APPROVE,
            P_BUDGET_READ, P_BUDGET_MANAGE,
            P_ASSET_READ, P_ASSET_MANAGE,
            P_INVENTORY_READ, P_INVENTORY_MANAGE,
            P_ORGANIZATION_READ, P_ORGANIZATION_MANAGE, P_AUDIT_READ,
            P_USERS_READ, P_USERS_MANAGE, P_ROLES_ASSIGN, P_SETTINGS_READ, P_SETTINGS_MANAGE,
            P_WORKFORCE_DASHBOARD, P_WORKERS_READ, P_WORKERS_CREATE, P_WORKERS_EDIT, P_WORKERS_DEACTIVATE,
            P_CONTRACTORS_READ, P_CONTRACTORS_MANAGE,
            P_LABOR_REQUESTS_READ, P_LABOR_REQUESTS_MANAGE, P_DISPATCH_DISPUTES_READ, P_DISPATCH_DISPUTES_MANAGE,
            P_ATTENDANCE_READ, P_ATTENDANCE_ENTER, P_ATTENDANCE_REVIEW, P_ATTENDANCE_IMPORT,
            P_SETTLEMENTS_READ, P_SETTLEMENTS_PREPARE, P_SETTLEMENTS_FINALIZE,
            P_ADVANCES_READ, P_ADVANCES_MANAGE,
            P_CONTRACTOR_ACCOUNTS_READ, P_CONTRACTOR_ACCOUNTS_MANAGE, P_WORKFORCE_REPORTS_READ,
            P_APPROVALS_READ, P_APPROVALS_DECIDE,
            P_WORKFLOW_DEFINITIONS_READ, P_WORKFLOW_DEFINITIONS_MANAGE,
            P_PROJECTS_READ, P_PROJECTS_MANAGE, P_PROJECTS_WBS_MANAGE, P_PROJECTS_CLOSE,
            P_LEAVES_READ, P_LEAVES_MANAGE,
            P_PERFORMANCE_READ, P_PERFORMANCE_MANAGE,
            P_ETA_TAX_READ, P_ETA_TAX_MANAGE,
            P_POS_READ, P_POS_OPERATE, P_POS_MANAGE,
            P_CRM_READ, P_CRM_MANAGE, P_CRM_OMNICHANNEL,
            P_VERTICALS_READ, P_VERTICALS_MANAGE);

    private static final Set<String> HR_READ = Set.of(
            P_DASHBOARD_VIEW, P_EMPLOYEES_READ, P_CATEGORIES_READ, P_IMPORTS_READ, P_PARTIES_READ,
            P_REPORTS_READ, P_PAYROLL_READ, P_ORGANIZATION_READ, P_LEAVES_READ, P_PERFORMANCE_READ);

    private static final Set<String> HR_WRITE = Set.of(
            P_EMPLOYEES_EDIT, P_EMPLOYEES_DEACTIVATE, P_CATEGORIES_MANAGE, P_IMPORTS_MANAGE,
            P_PARTIES_MANAGE, P_REPORTS_DECIDE, P_REPORTS_APPROVE, P_ORGANIZATION_MANAGE, P_LEAVES_MANAGE, P_PERFORMANCE_MANAGE);

    private static final Set<String> WORKFORCE_READ = Set.of(
            P_WORKFORCE_DASHBOARD, P_WORKERS_READ, P_CONTRACTORS_READ, P_LABOR_REQUESTS_READ, P_DISPATCH_DISPUTES_READ,
            P_ATTENDANCE_READ, P_SETTLEMENTS_READ, P_ADVANCES_READ,
            P_CONTRACTOR_ACCOUNTS_READ, P_WORKFORCE_REPORTS_READ, P_CATEGORIES_READ);

    private static final Set<String> FINANCE_READ = Set.of(P_FINANCE_READ, P_JOURNAL_READ, P_ETA_TAX_READ, P_ASSET_READ);
    private static final Set<String> FINANCE_WRITE = Set.of(P_FINANCE_MANAGE, P_JOURNAL_CREATE, P_JOURNAL_POST, P_ETA_TAX_MANAGE, P_ASSET_MANAGE);

    // ------------------------------------------------------------------
    // Page route-guard role matrices (mirrors the frontend route guards,
    // including ADMIN/SUPER_ADMIN which bypass every role guard) and the
    // tenant feature keys that gate each module's menu.
    // ------------------------------------------------------------------
    private static final Set<String> NO_ROLE_GUARD = Set.of();
    private static final Set<String> ADMIN_ONLY = Set.of("ADMIN", "SUPER_ADMIN");
    private static final Set<String> HR_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "HR_MANAGER");
    private static final Set<String> HR_REVIEW_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "HR_MANAGER", "HR_REVIEWER");
    private static final Set<String> PROCUREMENT_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "PROCUREMENT_MANAGER", "PROCUREMENT_USER",
            "INVENTORY_MANAGER", "FINANCE_MANAGER", "ACCOUNTANT", "TREASURY_USER", "AUDITOR");
    private static final Set<String> SALES_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "SALES_MANAGER");
    private static final Set<String> PRODUCTION_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "MANUFACTURING_MANAGER");
    private static final Set<String> QUALITY_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "MANUFACTURING_MANAGER", "QUALITY_MANAGER");
    private static final Set<String> PAYROLL_ROLES = Set.of("ADMIN", "SUPER_ADMIN", "PAYROLL_MANAGER", "HR_MANAGER", "HR_REVIEWER");
    private static final Set<String> FINANCE_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "FINANCE_MANAGER", "ACCOUNTANT", "TREASURY_USER", "AUDITOR");
    private static final Set<String> FINANCE_REPORT_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "FINANCE_MANAGER", "ACCOUNTANT", "AUDITOR");
    private static final Set<String> WORKFORCE_BASE_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "WORKFORCE_MANAGER", "WORKFORCE_REVIEWER", "WORKFORCE_FINANCE");
    private static final Set<String> WORKFORCE_IMPORT_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "WORKFORCE_MANAGER", "WORKFORCE_REVIEWER");
    private static final Set<String> WORKFORCE_ACCOUNT_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "WORKFORCE_MANAGER", "WORKFORCE_FINANCE");
    private static final Set<String> APPROVAL_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "WORKFORCE_MANAGER", "FINANCE_MANAGER", "PROCUREMENT_MANAGER",
            "HR_MANAGER", "WORKFORCE_REVIEWER", "ACCOUNTANT", "PROCUREMENT_USER");
    private static final Set<String> APPROVAL_PERMS = Set.of(P_APPROVALS_READ, P_APPROVALS_DECIDE);
    private static final Set<String> PROJECT_ROLES = Set.of(
            "ADMIN", "SUPER_ADMIN", "PROJECT_MANAGER", "FINANCE_MANAGER", "AUDITOR", "VIEWER");

    private static final String FEATURE_PAYROLL = "payroll.enabled";
    private static final String FEATURE_SALES = "sales.enabled";
    private static final String FEATURE_MANUFACTURING = "manufacturing.enabled";
    private static final String FEATURE_QUALITY = "quality.enabled";
    private static final String FEATURE_FINANCE = "finance.enabled";
    private static final String FEATURE_CONTRACTOR_ACCOUNTS = "workforce.contractorAccounts.enabled";

    private static final String KEY_ROLE_PREFIX = "roles.access.";
    private static final String KEY_PAGE_PREFIX = "access.pages.";
    private static final Set<String> SENSITIVE_PERMISSIONS = Set.of(
            P_JOURNAL_POST, P_JOURNAL_REVERSE, P_PAYROLL_APPROVE, P_PAYMENTS_EXECUTE,
            P_PAYMENTS_APPROVE, P_USERS_MANAGE, P_ROLES_ASSIGN, P_SETTINGS_MANAGE,
            P_SETTLEMENTS_FINALIZE, P_ATTENDANCE_REVIEW, P_PROJECTS_CLOSE);
    // ------------------------------------------------------------------
    // Role definitions (permissions derived from the enforced @PreAuthorize sets).
    // ------------------------------------------------------------------
    private final List<AccessRoleDef> roles = List.of(
            new AccessRoleDef("SUPER_ADMIN", key("superAdmin"), AccessSensitivity.CRITICAL,
                    RoleKind.ADMINISTRATION, ALL_PERMISSIONS, Set.of(), "access.sensitive.superAdmin"),
            new AccessRoleDef("ADMIN", key("admin"), AccessSensitivity.CRITICAL,
                    RoleKind.ADMINISTRATION, ALL_PERMISSIONS, Set.of(), "access.sensitive.admin"),
            new AccessRoleDef("HR_MANAGER", key("hrManager"), AccessSensitivity.HIGH,
                    RoleKind.OPERATIONAL,
                    union(HR_READ, HR_WRITE, APPROVAL_PERMS,
                            Set.of(P_PAYROLL_PREPARE, P_PAYROLL_APPROVE)),
                    Set.of(), "access.sensitive.hrManager"),
            new AccessRoleDef("HR_REVIEWER", key("hrReviewer"), AccessSensitivity.MEDIUM,
                    RoleKind.APPROVAL,
                    union(HR_READ, Set.of(P_ATTENDANCE_REVIEW, P_REPORTS_DECIDE)),
                    Set.of(), "access.sensitive.hrReviewer"),
            new AccessRoleDef("VIEWER", key("viewer"), AccessSensitivity.LOW,
                    RoleKind.READ_ONLY,
                    Set.of(P_DASHBOARD_VIEW, P_REPORTS_READ, P_SETTINGS_READ, P_PROJECTS_READ),
                    Set.of(), null),
            new AccessRoleDef("PROJECT_MANAGER", key("projectManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    union(Set.of(P_PROJECTS_READ, P_PROJECTS_MANAGE, P_PROJECTS_WBS_MANAGE, P_PROJECTS_CLOSE,
                            P_PROCUREMENT_READ, P_INVENTORY_READ, P_BUDGET_READ), APPROVAL_PERMS),
                    Set.of(), "access.sensitive.projectManager"),
            new AccessRoleDef("FINANCE_MANAGER", key("financeManager"), AccessSensitivity.HIGH,
                    RoleKind.FINANCE,
                    union(FINANCE_READ, FINANCE_WRITE, APPROVAL_PERMS,
                            Set.of(P_JOURNAL_REVERSE, P_PROCUREMENT_READ, P_PAYMENTS_EXECUTE, P_AUDIT_READ,
                                    P_BUDGET_READ, P_BUDGET_MANAGE, P_PROJECTS_READ)),
                    Set.of(), "access.sensitive.financeManager"),
            new AccessRoleDef("ACCOUNTANT", key("accountant"), AccessSensitivity.MEDIUM,
                    RoleKind.FINANCE,
                    union(FINANCE_READ, FINANCE_WRITE, APPROVAL_PERMS,
                            Set.of(P_PROCUREMENT_READ, P_PAYMENTS_EXECUTE, P_BUDGET_READ)),
                    Set.of(), "access.sensitive.accountant"),
            new AccessRoleDef("TREASURY_USER", key("treasuryUser"), AccessSensitivity.MEDIUM,
                    RoleKind.FINANCE,
                    union(FINANCE_READ, Set.of(P_PROCUREMENT_READ, P_BUDGET_READ)),
                    Set.of(), "access.sensitive.treasuryUser"),
            new AccessRoleDef("PROCUREMENT_MANAGER", key("procurementManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    union(Set.of(P_PROCUREMENT_READ, P_PROCUREMENT_MANAGE, P_INVENTORY_READ,
                            P_PAYMENTS_EXECUTE, P_PAYMENTS_APPROVE), APPROVAL_PERMS),
                    Set.of(), "access.sensitive.procurementManager"),
            new AccessRoleDef("PROCUREMENT_USER", key("procurementUser"), AccessSensitivity.LOW,
                    RoleKind.OPERATIONAL,
                    union(Set.of(P_PROCUREMENT_READ, P_INVENTORY_READ), APPROVAL_PERMS),
                    Set.of(), null),
            new AccessRoleDef("SALES_MANAGER", key("salesManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    Set.of(P_SALES_READ, P_SALES_MANAGE),
                    Set.of(), null),
            new AccessRoleDef("INVENTORY_MANAGER", key("inventoryManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    Set.of(P_PROCUREMENT_READ, P_INVENTORY_READ, P_INVENTORY_MANAGE),
                    Set.of(), null),
            new AccessRoleDef("MANUFACTURING_MANAGER", key("manufacturingManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    Set.of(P_MANUFACTURING_READ, P_MANUFACTURING_MANAGE, P_QUALITY_READ),
                    Set.of(), null),
            new AccessRoleDef("QUALITY_MANAGER", key("qualityManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    Set.of(P_QUALITY_READ, P_QUALITY_MANAGE),
                    Set.of(), null),
            new AccessRoleDef("PAYROLL_MANAGER", key("payrollManager"), AccessSensitivity.HIGH,
                    RoleKind.APPROVAL,
                    Set.of(P_PAYROLL_READ, P_PAYROLL_PREPARE, P_PAYROLL_APPROVE),
                    Set.of(), "access.sensitive.payrollManager"),
            new AccessRoleDef("WORKFORCE_MANAGER", key("workforceManager"), AccessSensitivity.MEDIUM,
                    RoleKind.OPERATIONAL,
                    union(WORKFORCE_READ, APPROVAL_PERMS,
                            Set.of(P_WORKERS_CREATE, P_WORKERS_EDIT, P_WORKERS_DEACTIVATE,
                                    P_CONTRACTORS_MANAGE, P_LABOR_REQUESTS_MANAGE, P_DISPATCH_DISPUTES_MANAGE,
                                    P_ATTENDANCE_ENTER, P_ATTENDANCE_REVIEW, P_ATTENDANCE_IMPORT,
                                    P_SETTLEMENTS_PREPARE, P_SETTLEMENTS_FINALIZE,
                                    P_ADVANCES_MANAGE, P_CONTRACTOR_ACCOUNTS_MANAGE, P_CATEGORIES_MANAGE)),
                    Set.of(), "access.sensitive.workforceManager"),
            new AccessRoleDef("WORKFORCE_REVIEWER", key("workforceReviewer"), AccessSensitivity.MEDIUM,
                    RoleKind.APPROVAL,
                    union(without(WORKFORCE_READ, Set.of(P_CONTRACTOR_ACCOUNTS_READ)), APPROVAL_PERMS,
                            Set.of(P_ATTENDANCE_REVIEW, P_ATTENDANCE_IMPORT)),
                    Set.of(), "access.sensitive.workforceReviewer"),
            new AccessRoleDef("WORKFORCE_FINANCE", key("workforceFinance"), AccessSensitivity.MEDIUM,
                    RoleKind.FINANCE,
                    union(without(WORKFORCE_READ, Set.of(P_WORKFORCE_REPORTS_READ)),
                            Set.of(P_SETTLEMENTS_PREPARE, P_ADVANCES_MANAGE, P_CONTRACTOR_ACCOUNTS_MANAGE)),
                    Set.of(), "access.sensitive.workforceFinance"),
            new AccessRoleDef("AUDITOR", key("auditor"), AccessSensitivity.LOW,
                    RoleKind.READ_ONLY,
                    Set.of(P_AUDIT_READ, P_FINANCE_READ, P_JOURNAL_READ, P_PROCUREMENT_READ,
                            P_SALES_READ, P_MANUFACTURING_READ, P_QUALITY_READ, P_OPERATIONS_READ,
                            P_INVENTORY_READ, P_BUDGET_READ, P_PROJECTS_READ),
                    Set.of(), null));
    // ------------------------------------------------------------------
    // Page definitions (menuId matches the shell navigation and users.page).
    // ------------------------------------------------------------------
    private final List<AccessPageDef> pages = List.of(
            page("DASHBOARD", "DASHBOARD", "/dashboard", "dashboard", "nav.dashboard", P_DASHBOARD_VIEW,
                    NO_ROLE_GUARD, null),
            page("PROJECTS", "PROJECTS", "/projects", "projects", "nav.projects", P_PROJECTS_READ,
                    PROJECT_ROLES, null,
                    action("MANAGE", P_PROJECTS_MANAGE, false),
                    action("WBS_MANAGE", P_PROJECTS_WBS_MANAGE, false),
                    action("CLOSE", P_PROJECTS_CLOSE, true)),
            page("EMPLOYEES", "HR", "/employees", "employees", "nav.employees", P_EMPLOYEES_READ,
                    HR_ROLES, null,
                    action("EDIT", P_EMPLOYEES_EDIT, false),
                    action("DEACTIVATE", P_EMPLOYEES_DEACTIVATE, false)),
            page("LEAVES", "HR", "/leaves", "leaves", "nav.leaves", P_LEAVES_READ,
                    HR_REVIEW_ROLES, null,
                    action("MANAGE", P_LEAVES_MANAGE, false)),
            page("PERFORMANCE", "HR", "/performance", "performance", "nav.performance", P_PERFORMANCE_READ,
                    HR_REVIEW_ROLES, null,
                    action("MANAGE", P_PERFORMANCE_MANAGE, false)),
            page("CATEGORIES", "HR", "/categories", "categories", "nav.categories", P_CATEGORIES_READ,
                    HR_ROLES, null,
                    action("MANAGE", P_CATEGORIES_MANAGE, false)),
            page("IMPORTS", "HR", "/imports", "imports", "nav.imports", P_IMPORTS_READ,
                    HR_REVIEW_ROLES, null,
                    action("MANAGE", P_IMPORTS_MANAGE, false)),
            page("PARTIES", "HR", "/parties", "parties", "nav.parties", P_PARTIES_READ,
                    HR_ROLES, null,
                    action("MANAGE", P_PARTIES_MANAGE, false)),
            page("REPORTS", "HR", "/reports", "reports", "nav.reports", P_REPORTS_READ,
                    NO_ROLE_GUARD, null,
                    action("DECIDE", P_REPORTS_DECIDE, false),
                    action("APPROVE", P_REPORTS_APPROVE, true)),
            page("EXECUTIVE_ANALYTICS", "REPORTS", "/analytics/executive", "executive-analytics", "nav.executiveAnalytics",
                    P_REPORTS_READ, PROJECT_ROLES, null),
            page("OPERATIONS", "OPERATIONS", "/operations", "operations", "nav.operations", P_OPERATIONS_READ,
                    ADMIN_ONLY, null,
                    action("MANAGE", P_OPERATIONS_MANAGE, false)),
            page("PROCUREMENT", "TRADE", "/trade/procurement", "procurement", "nav.procurement", P_PROCUREMENT_READ,
                    PROCUREMENT_ROLES, null,
                    action("MANAGE", P_PROCUREMENT_MANAGE, false)),
            page("SALES", "TRADE", "/trade/sales", "sales", "nav.sales", P_SALES_READ,
                    SALES_ROLES, FEATURE_SALES,
                    action("MANAGE", P_SALES_MANAGE, false)),
            page("POS", "TRADE", "/trade/pos", "pos", "pos.title", P_POS_READ,
                    SALES_ROLES, FEATURE_SALES,
                    action("OPERATE", P_POS_OPERATE, false),
                    action("MANAGE", P_POS_MANAGE, true)),
            page("CRM", "TRADE", "/crm", "crm", "nav.crm", P_CRM_READ,
                    SALES_ROLES, FEATURE_SALES,
                    action("MANAGE", P_CRM_MANAGE, false),
                    action("OMNICHANNEL", P_CRM_OMNICHANNEL, false)),
            page("PRODUCTION", "MANUFACTURING", "/manufacturing/production", "production", "nav.production",
                    P_MANUFACTURING_READ, PRODUCTION_ROLES, FEATURE_MANUFACTURING,
                    action("MANAGE", P_MANUFACTURING_MANAGE, false)),
            page("QUALITY", "MANUFACTURING", "/manufacturing/quality", "quality", "nav.quality", P_QUALITY_READ,
                    QUALITY_ROLES, FEATURE_QUALITY,
                    action("MANAGE", P_QUALITY_MANAGE, false)),
            page("PAYROLL", "PAYROLL", "/payroll", "payroll", "nav.payroll", P_PAYROLL_READ,
                    PAYROLL_ROLES, FEATURE_PAYROLL,
                    action("PREPARE", P_PAYROLL_PREPARE, false),
                    action("APPROVE", P_PAYROLL_APPROVE, true)),
            page("ACCOUNTS", "FINANCE", "/finance/accounts", "accounts", "nav.accounts", P_FINANCE_READ,
                    FINANCE_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_FINANCE_MANAGE, false)),
            page("JOURNAL_ENTRIES", "FINANCE", "/finance/journal-entries", "journal-entries", "nav.journalEntries",
                    P_JOURNAL_READ, FINANCE_ROLES, FEATURE_FINANCE,
                    action("CREATE", P_JOURNAL_CREATE, false),
                    action("POST", P_JOURNAL_POST, true),
                    action("REVERSE", P_JOURNAL_REVERSE, true)),
            page("BANKS", "FINANCE", "/finance/banks", "banks", "nav.banks", P_FINANCE_READ,
                    FINANCE_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_FINANCE_MANAGE, false)),
            page("TAX_CURRENCY", "FINANCE", "/finance/tax-currency", "tax-currency", "nav.taxCurrency", P_FINANCE_READ,
                    FINANCE_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_FINANCE_MANAGE, false)),
            page("FISCAL_PERIODS", "FINANCE", "/fiscal-periods", "fiscal-periods", "nav.fiscalPeriods", P_FINANCE_READ,
                    FINANCE_REPORT_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_FINANCE_MANAGE, false)),
            page("BUDGETS", "FINANCE", "/finance/budgets", "budgets", "nav.budgets", P_BUDGET_READ,
                    FINANCE_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_BUDGET_MANAGE, false)),
            page("FIXED_ASSETS", "FINANCE", "/finance/fixed-assets", "fixed-assets", "nav.fixedAssets",
                    P_ASSET_READ, FINANCE_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_ASSET_MANAGE, false)),
            page("ETA_TAX", "FINANCE", "/compliance/eta-tax", "eta-tax", "nav.etaTax", P_ETA_TAX_READ,
                    FINANCE_ROLES, FEATURE_FINANCE,
                    action("MANAGE", P_ETA_TAX_MANAGE, false)),
            page("ORGANIZATION", "HR", "/organization", "organization", "nav.organization",
                    P_ORGANIZATION_READ, HR_ROLES, null,
                    action("MANAGE", P_ORGANIZATION_MANAGE, false)),
            page("AUDIT_LOGS", "ADMINISTRATION", "/audit-logs", "audit-logs", "nav.auditLogs", P_AUDIT_READ,
                    ADMIN_ONLY, null),
            page("USERS", "ADMINISTRATION", "/users", "users", "nav.users", P_USERS_READ,
                    ADMIN_ONLY, null,
                    action("MANAGE", P_USERS_MANAGE, true),
                    action("ASSIGN_ROLES", P_ROLES_ASSIGN, true)),
            page("SETTINGS", "ADMINISTRATION", "/settings", "settings", "settings.title", P_SETTINGS_READ,
                    NO_ROLE_GUARD, null,
                    action("MANAGE", P_SETTINGS_MANAGE, true)),
            page("WORKFORCE_DASHBOARD", "WORKFORCE", "/workforce/dashboard", "workforce-dashboard",
                    "workforce.dashboard.title", P_WORKFORCE_DASHBOARD, WORKFORCE_BASE_ROLES, null),
            page("WORKFORCE_CONTRACTORS", "WORKFORCE", "/workforce/contractors", "workforce-contractors",
                    "workforce.contractors.title", P_CONTRACTORS_READ, WORKFORCE_BASE_ROLES, null,
                    action("MANAGE", P_CONTRACTORS_MANAGE, false)),
            page("WORKFORCE_WORKERS", "WORKFORCE", "/workforce/workers", "workforce-workers",
                    "workforce.workers.title", P_WORKERS_READ, WORKFORCE_BASE_ROLES, null,
                    action("CREATE", P_WORKERS_CREATE, false),
                    action("EDIT", P_WORKERS_EDIT, false),
                    action("DEACTIVATE", P_WORKERS_DEACTIVATE, false)),
            page("WORKFORCE_CATEGORIES", "WORKFORCE", "/workforce/categories", "workforce-categories",
                    "workforce.categories.title", P_CATEGORIES_READ, WORKFORCE_BASE_ROLES, null,
                    action("MANAGE", P_CATEGORIES_MANAGE, false)),
            page("WORKFORCE_REQUESTS", "WORKFORCE", "/workforce/labor-requests", "workforce-requests",
                    "workforce.laborRequests.title", P_LABOR_REQUESTS_READ, WORKFORCE_BASE_ROLES, null,
                    action("MANAGE", P_LABOR_REQUESTS_MANAGE, false)),
            page("WORKFORCE_ATTENDANCE", "WORKFORCE", "/workforce/attendance", "workforce-attendance",
                    "workforce.attendance.title", P_ATTENDANCE_READ, WORKFORCE_BASE_ROLES, null,
                    action("ENTER", P_ATTENDANCE_ENTER, false),
                    action("REVIEW", P_ATTENDANCE_REVIEW, true)),
            page("WORKFORCE_DISPATCH_DISPUTES", "WORKFORCE", "/workforce/dispatch-disputes", "workforce-dispatch-disputes",
                    "workforce.dispatch.title", P_DISPATCH_DISPUTES_READ, WORKFORCE_BASE_ROLES, null,
                    action("MANAGE", P_DISPATCH_DISPUTES_MANAGE, false)),
            page("WORKFORCE_SETTLEMENTS", "WORKFORCE", "/workforce/settlement-periods", "workforce-settlements",
                    "workforce.settlements.title", P_SETTLEMENTS_READ, WORKFORCE_BASE_ROLES,
                    FEATURE_CONTRACTOR_ACCOUNTS,
                    action("PREPARE", P_SETTLEMENTS_PREPARE, false),
                    action("FINALIZE", P_SETTLEMENTS_FINALIZE, true)),
            page("WORKFORCE_ADVANCES", "WORKFORCE", "/workforce/advances", "workforce-advances",
                    "workforce.advances.title", P_ADVANCES_READ, WORKFORCE_BASE_ROLES, null,
                    action("MANAGE", P_ADVANCES_MANAGE, false)),
            page("WORKFORCE_ACCOUNTS", "WORKFORCE", "/workforce/contractor-accounts", "workforce-accounts",
                    "workforce.accounts.title", P_CONTRACTOR_ACCOUNTS_READ, WORKFORCE_ACCOUNT_ROLES,
                    FEATURE_CONTRACTOR_ACCOUNTS,
                    action("MANAGE", P_CONTRACTOR_ACCOUNTS_MANAGE, false)),
            page("WORKFORCE_REPORTS", "WORKFORCE", "/workforce/reports-import", "workforce-reports",
                    "workforce.reports.title", P_WORKFORCE_REPORTS_READ, WORKFORCE_IMPORT_ROLES, null,
                    action("IMPORT", P_ATTENDANCE_IMPORT, false)),
            page("PENDING_APPROVALS", "APPROVALS", "/approvals/my-tasks", "approvals-my-tasks",
                    "approvals.myTasks", P_APPROVALS_READ, NO_ROLE_GUARD, null,
                    action("DECIDE", P_APPROVALS_DECIDE, false)),
            page("WORKFLOW_DEFINITIONS", "APPROVALS", "/approvals/definitions", "approvals-workflows",
                    "approvals.workflows", P_WORKFLOW_DEFINITIONS_READ, ADMIN_ONLY, null,
                    action("MANAGE", P_WORKFLOW_DEFINITIONS_MANAGE, true)),
            page("NOTIFICATIONS_SEND", "ADMINISTRATION", "/notifications/send", "notifications-send",
                    "nav.notificationsSend", P_USERS_READ, ADMIN_ONLY, null),
            page("SPECIALIZED_VERTICALS", "VERTICALS", "/verticals/specialized", "specialized-verticals",
                    "nav.specializedVerticals", P_VERTICALS_READ, NO_ROLE_GUARD, null,
                    action("MANAGE", P_VERTICALS_MANAGE, false)));
    // ------------------------------------------------------------------
    // Segregation-of-duties rules.
    // ------------------------------------------------------------------
    private final List<AccessConflictRuleDef> conflictRules = List.of(
            rule("JOURNAL_CREATE_AND_POST", List.of(P_JOURNAL_CREATE, P_JOURNAL_POST),
                    ConflictSeverity.WARNING, "access.conflicts.journalCreateAndPost"),
            rule("JOURNAL_CREATE_AND_REVERSE", List.of(P_JOURNAL_CREATE, P_JOURNAL_REVERSE),
                    ConflictSeverity.WARNING, "access.conflicts.journalCreateAndReverse"),
            rule("PAYROLL_PREPARE_AND_APPROVE", List.of(P_PAYROLL_PREPARE, P_PAYROLL_APPROVE),
                    ConflictSeverity.WARNING, "access.conflicts.payrollPrepareAndApprove"),
            rule("SETTLEMENT_PREPARE_AND_FINALIZE", List.of(P_SETTLEMENTS_PREPARE, P_SETTLEMENTS_FINALIZE),
                    ConflictSeverity.WARNING, "access.conflicts.settlementPrepareAndFinalize"),
            rule("PAYMENTS_CREATE_AND_APPROVE", List.of(P_PAYMENTS_EXECUTE, P_PAYMENTS_APPROVE),
                    ConflictSeverity.WARNING, "access.conflicts.paymentsCreateAndApprove"));
    // ------------------------------------------------------------------
    // Business needs used by the guided "what should this user be able to do?" mode.
    // ------------------------------------------------------------------
    private final List<AccessNeedDef> needs = List.of(
            need("VIEW_DASHBOARD", Set.of(P_DASHBOARD_VIEW)),
            need("VIEW_PROJECTS", Set.of(P_PROJECTS_READ)),
            need("MANAGE_PROJECTS", Set.of(P_PROJECTS_READ, P_PROJECTS_MANAGE, P_PROJECTS_WBS_MANAGE)),
            need("VIEW_REPORTS", Set.of(P_REPORTS_READ)),
            need("MANAGE_EMPLOYEES", Set.of(P_EMPLOYEES_READ, P_EMPLOYEES_EDIT, P_EMPLOYEES_DEACTIVATE)),
            need("VIEW_WORKERS", Set.of(P_WORKERS_READ)),
            need("ADD_WORKERS", Set.of(P_WORKERS_READ, P_WORKERS_CREATE)),
            need("MANAGE_WORKERS", Set.of(P_WORKERS_READ, P_WORKERS_CREATE, P_WORKERS_EDIT, P_WORKERS_DEACTIVATE)),
            need("VIEW_CONTRACTORS", Set.of(P_CONTRACTORS_READ)),
            need("MANAGE_CONTRACTORS", Set.of(P_CONTRACTORS_READ, P_CONTRACTORS_MANAGE)),
            need("REVIEW_ATTENDANCE", Set.of(P_ATTENDANCE_READ, P_ATTENDANCE_REVIEW)),
            need("MANAGE_ATTENDANCE", Set.of(P_ATTENDANCE_READ, P_ATTENDANCE_ENTER, P_ATTENDANCE_REVIEW)),
            need("PREPARE_SETTLEMENTS", Set.of(P_SETTLEMENTS_READ, P_SETTLEMENTS_PREPARE)),
            need("FINALIZE_SETTLEMENTS", Set.of(P_SETTLEMENTS_READ, P_SETTLEMENTS_PREPARE, P_SETTLEMENTS_FINALIZE)),
            need("MANAGE_ADVANCES", Set.of(P_ADVANCES_READ, P_ADVANCES_MANAGE)),
            need("PREPARE_PAYROLL", Set.of(P_PAYROLL_READ, P_PAYROLL_PREPARE)),
            need("APPROVE_PAYROLL", Set.of(P_PAYROLL_READ, P_PAYROLL_PREPARE, P_PAYROLL_APPROVE)),
            need("CREATE_JOURNAL", Set.of(P_JOURNAL_READ, P_JOURNAL_CREATE)),
            need("POST_JOURNAL", Set.of(P_JOURNAL_READ, P_JOURNAL_POST)),
            need("REVERSE_JOURNAL", Set.of(P_JOURNAL_READ, P_JOURNAL_REVERSE)),
            need("VIEW_FINANCE", Set.of(P_FINANCE_READ, P_JOURNAL_READ)),
            need("MANAGE_PROCUREMENT", Set.of(P_PROCUREMENT_READ, P_PROCUREMENT_MANAGE)),
            need("MANAGE_APPROVALS", Set.of(P_APPROVALS_READ, P_APPROVALS_DECIDE)),
            need("MANAGE_USERS", Set.of(P_USERS_READ, P_USERS_MANAGE, P_ROLES_ASSIGN)),
            need("VIEW_AUDIT", Set.of(P_AUDIT_READ)));

    private final Map<String, AccessRoleDef> roleByCode;
    private final Map<String, AccessPageDef> pageByCode;
    private final Map<String, Set<String>> permissionRoles;
    private final Map<String, Set<String>> rolePermissions;

    public AccessCatalog() {
        this.roleByCode = roles.stream().collect(Collectors.toUnmodifiableMap(AccessRoleDef::code, r -> r));
        this.pageByCode = pages.stream().collect(Collectors.toUnmodifiableMap(AccessPageDef::code, p -> p));
        this.permissionRoles = buildPermissionRoles();
        this.rolePermissions = roles.stream().collect(Collectors.toUnmodifiableMap(
                AccessRoleDef::code, r -> Set.copyOf(r.permissions())));
    }

    private static String key(String role) {
        return KEY_ROLE_PREFIX + role;
    }

    private static AccessActionDef action(String code, String permission, boolean sensitive) {
        return new AccessActionDef(code, permission, sensitive);
    }

    private static AccessPageDef page(String code, String module, String route, String menuId, String titleKey,
                                      String viewPermission, Set<String> requiredRoles, String requiredFeature,
                                      AccessActionDef... actions) {
        return new AccessPageDef(code, module, route, menuId, titleKey, viewPermission,
                requiredRoles, requiredFeature, List.of(actions));
    }

    private static AccessConflictRuleDef rule(String code, List<String> permissions,
                                              ConflictSeverity severity, String reasonKey) {
        return new AccessConflictRuleDef(code, permissions, severity, reasonKey);
    }

    private static AccessNeedDef need(String code, Set<String> permissions) {
        return new AccessNeedDef(code, KEY_PAGE_PREFIX + "needs." + code.toLowerCase().replace('_', '-'),
                permissions);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new HashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }

    private static Set<String> union(Set<String> first, Set<String> second, Set<String> third) {
        Set<String> result = new HashSet<>(first);
        result.addAll(second);
        result.addAll(third);
        return Set.copyOf(result);
    }

    private static Set<String> union(Set<String> first, Set<String> second,
                                     Set<String> third, Set<String> fourth) {
        Set<String> result = new HashSet<>(first);
        result.addAll(second);
        result.addAll(third);
        result.addAll(fourth);
        return Set.copyOf(result);
    }

    private static Set<String> without(Set<String> base, Set<String> excluded) {
        Set<String> result = new HashSet<>(base);
        result.removeAll(excluded);
        return Set.copyOf(result);
    }

    private Map<String, Set<String>> buildPermissionRoles() {
        Map<String, Set<String>> byPermission = new LinkedHashMap<>();
        for (AccessRoleDef role : roles) {
            for (String permission : role.permissions()) {
                byPermission.computeIfAbsent(permission, ignored -> new HashSet<>()).add(role.code());
            }
        }
        Map<String, Set<String>> immutable = new LinkedHashMap<>();
        byPermission.forEach((permission, codes) -> immutable.put(permission, Collections.unmodifiableSet(codes)));
        return Collections.unmodifiableMap(immutable);
    }

    public List<AccessRoleDef> roles() {
        return roles;
    }

    public List<AccessPageDef> pages() {
        return pages;
    }

    public List<AccessConflictRuleDef> conflictRules() {
        return conflictRules;
    }

    public List<AccessNeedDef> needs() {
        return needs;
    }

    public AccessRoleDef role(String code) {
        return roleByCode.get(code);
    }

    public boolean hasRole(String code) {
        return roleByCode.containsKey(code);
    }

    public AccessPageDef page(String code) {
        return pageByCode.get(code);
    }

    /**
     * All roles that grant a given permission.
     */
    public Set<String> rolesGranting(String permission) {
        return permissionRoles.getOrDefault(permission, Set.of());
    }

    /**
     * Permissions granted by a single role code.
     */
    public Set<String> permissionsOf(String roleCode) {
        return rolePermissions.getOrDefault(roleCode, Set.of());
    }

    /**
     * Union of permissions granted by a set of role codes.
     */
    public Set<String> permissionsOfRoles(Set<String> roleCodes) {
        Set<String> union = new HashSet<>();
        for (String code : roleCodes) {
            union.addAll(permissionsOf(code));
        }
        return union;
    }

    public Set<String> sensitivePermissions() {
        return SENSITIVE_PERMISSIONS;
    }
}
