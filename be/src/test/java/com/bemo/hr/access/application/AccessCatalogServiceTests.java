package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessApi;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureRepository;
import com.bemo.hr.shared.security.TenantFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the role-to-page access guidance: catalog integrity, effective
 * access preview, segregation-of-duties evaluation and the authoritative
 * assignment validation used before a user is saved.
 */
class AccessCatalogServiceTests {

    private final AccessCatalog catalog = new AccessCatalog();
    private final TenantFeatureRepository featureRepository = mock(TenantFeatureRepository.class);
    private final TenantFeatureService tenantFeatureService = new TenantFeatureService(featureRepository, new com.bemo.hr.shared.security.EntitlementCatalog());
    private final AccessCatalogService service = new AccessCatalogService(catalog, tenantFeatureService);

    @BeforeEach
    void setUp() {
        TenantContext.set("test-app");
        when(featureRepository.findByAppId(anyString())).thenReturn(List.of());
        when(featureRepository.findById(any())).thenReturn(java.util.Optional.empty());
    }

    private void enableAllFeatures() {
        Map<String, Boolean> features = new HashMap<>();
        for (String key : List.of("payroll.enabled", "sales.enabled", "manufacturing.enabled",
                "quality.enabled", "finance.enabled", "workforce.contractorAccounts.enabled",
                "agri.enabled", "medical.enabled")) {
            features.put(key, true);
        }
        when(featureRepository.findByAppId(anyString())).thenAnswer(
                invocation -> features.entrySet().stream()
                        .map(entry -> new com.bemo.hr.shared.security.TenantFeature(
                                "test-app", entry.getKey(), entry.getValue(), null, "test"))
                        .toList());
    }

    private void disableFeature(String key) {
        when(featureRepository.findByAppId(anyString())).thenReturn(List.of(
                new com.bemo.hr.shared.security.TenantFeature(
                        "test-app", key, false, null, "test")));
    }

    @Test
    void catalogExposesEveryRolePageAndRule() {
        var response = service.catalog();

        assertThat(response.roles()).hasSize(20);
        assertThat(response.roles()).extracting(AccessApi.AccessRoleResponse::code)
                .contains("SUPER_ADMIN", "ADMIN", "HR_MANAGER", "WORKFORCE_MANAGER", "PROJECT_MANAGER", "AUDITOR");
        assertThat(response.pages()).isNotEmpty();
        assertThat(response.pages()).allSatisfy(page -> {
            assertThat(page.menuId()).isNotBlank();
            assertThat(page.roles()).isNotNull();
        });
        assertThat(response.conflictRules()).hasSize(5);
        assertThat(response.conflictRules()).allSatisfy(rule ->
                assertThat(rule.permissions()).isNotEmpty());
        assertThat(response.sensitivePermissions()).contains("journal.post", "users.manage", "projects.close");
        assertThat(response.needs()).isNotEmpty();
        assertThat(response.needs()).allSatisfy(need ->
                assertThat(need.permissions()).isNotEmpty());
    }

    @Test
    void viewerIsHiddenWhenMenuIsNotGranted() {
        var preview = service.preview(List.of("VIEWER"), List.of("dashboard"));

        var dashboard = page(preview, "DASHBOARD");
        assertThat(dashboard.access()).isEqualTo("VIEW");
        assertThat(dashboard.grantedByRoles()).containsExactly("VIEWER");

        var users = page(preview, "USERS");
        assertThat(users.access()).isEqualTo("HIDDEN");
    }

    @Test
    void missingViewPermissionYieldsRestricted() {
        var preview = service.preview(List.of("VIEWER"), List.of("users"));

        var users = page(preview, "USERS");
        assertThat(users.access()).isEqualTo("RESTRICTED");
        assertThat(users.missingPermissions()).contains("users.read");
    }

    @Test
    void actionsDeriveAccessLevelByPrecedence() {
        enableAllFeatures();
        var preview = service.preview(List.of("HR_MANAGER"), List.of("payroll"));

        var payroll = page(preview, "PAYROLL");
        assertThat(payroll.grantedActions()).contains("PREPARE", "APPROVE");
        assertThat(payroll.access()).isEqualTo("APPROVE");

        var journal = page(preview, "JOURNAL_ENTRIES");
        assertThat(journal.access()).isEqualTo("HIDDEN");
    }

    @Test
    void sensitiveWarningsAreReturnedForGrantedSensitivePermissions() {
        var preview = service.preview(List.of("FINANCE_MANAGER"), List.of("journal-entries", "payments"));

        assertThat(preview.warnings()).extracting(AccessApi.AccessWarningResponse::code)
                .contains("journal.post", "payments.execute");
        assertThat(preview.sensitivePermissions()).contains("journal.post");
    }

    @Test
    void segregationOfDutiesConflictsAreReported() {
        var preview = service.preview(List.of("PAYROLL_MANAGER"), List.of("payroll"));

        assertThat(preview.conflicts()).extracting(AccessApi.AccessConflictResponse::code)
                .contains("PAYROLL_PREPARE_AND_APPROVE");
        assertThat(preview.conflicts()).extracting(AccessApi.AccessConflictResponse::severity)
                .contains("WARNING");

        var procurement = service.preview(List.of("PROCUREMENT_MANAGER"), List.of("procurement"));
        assertThat(procurement.conflicts()).extracting(AccessApi.AccessConflictResponse::code)
                .contains("PAYMENTS_CREATE_AND_APPROVE");
    }

    @Test
    void unknownRoleIsRejected() {
        assertThatThrownBy(() -> service.preview(List.of("NO_SUCH_ROLE"), List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Unknown role");
        assertThatThrownBy(() -> service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("NO_SUCH_ROLE"), List.of(), null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Unknown role");
    }

    @Test
    void nonSuperAdminCannotAssignSuperAdmin() {
        assertThatThrownBy(() -> service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("SUPER_ADMIN"), List.of(), null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Super Admin");
    }

    @Test
    void selfRoleModificationIsForbidden() {
        assertThatThrownBy(() -> service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER", "ADMIN"), List.of(),
                "actor-1", Set.of("VIEWER"), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("own roles");
    }

    @Test
    void unchangedSelfRolesAreAllowed() {
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER", "ADMIN"), List.of(),
                "actor-1", Set.of("ADMIN", "VIEWER"), null);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void warningConflictsDoNotBlockValidation() {
        enableAllFeatures();
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("PAYROLL_MANAGER"), List.of("payroll"),
                null, null, null);
        assertThat(result.valid()).isTrue();
        assertThat(result.conflicts()).extracting(AccessApi.AccessConflictResponse::code)
                .contains("PAYROLL_PREPARE_AND_APPROVE");
    }

    @Test
    void suggestedRolesCoverNeedsAndExcludeAdministration() {
        var suggested = service.suggestRoles(Set.of("workers.read", "workers.create", "workers.edit"));

        assertThat(suggested).contains("WORKFORCE_MANAGER");
        assertThat(suggested).doesNotContain("ADMIN", "SUPER_ADMIN");

        var empty = service.suggestRoles(Set.of());
        assertThat(empty).isEmpty();
    }

    @Test
    void viewerGrantsOnlyDashboardReportsAndSettings() {
        assertThat(catalog.permissionsOf("VIEWER"))
                .containsExactlyInAnyOrder("dashboard.view", "reports.read", "settings.read", "projects.read");
    }

    @Test
    void hrRolesNoLongerGrantWorkforcePermissions() {
        assertThat(catalog.permissionsOf("HR_MANAGER")).noneMatch(permission -> permission.startsWith("workers."));
        assertThat(catalog.permissionsOf("HR_MANAGER")).doesNotContain("attendance.review", "attendance.import");
        assertThat(catalog.permissionsOf("HR_REVIEWER")).noneMatch(permission -> permission.startsWith("workers."));
    }

    @Test
    void workforceReviewerLosesContractorAccountRead() {
        assertThat(catalog.permissionsOf("WORKFORCE_REVIEWER"))
                .doesNotContain("contractorAccounts.read", "contractorAccounts.manage");
    }

    @Test
    void workforceFinanceLosesWorkforceReportsRead() {
        assertThat(catalog.permissionsOf("WORKFORCE_FINANCE")).doesNotContain("workforceReports.read");
    }

    @Test
    void treasuryUserCannotCreateOrPostJournals() {
        assertThat(catalog.permissionsOf("TREASURY_USER"))
                .doesNotContain("journal.create", "journal.post")
                .contains("procurement.read");
    }

    @Test
    void disabledFeatureYieldsModuleUnavailable() {
        disableFeature("payroll.enabled");
        var preview = service.preview(List.of("PAYROLL_MANAGER"), List.of("payroll"));

        var payroll = page(preview, "PAYROLL");
        assertThat(payroll.access()).isEqualTo("MODULE_UNAVAILABLE");
    }

    @Test
    void validateReportsUnknownMenu() {
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER"), List.of("no-such-menu"),
                null, null, null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .contains("ACCESS_UNKNOWN_MENU");
    }

    @Test
    void validateReportsMenuRoleMismatch() {
        enableAllFeatures();
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER"), List.of("payroll"),
                null, null, null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .contains("ACCESS_MENU_ROLE_MISMATCH");
    }

    @Test
    void validateReportsFeatureDisabled() {
        disableFeature("payroll.enabled");
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("PAYROLL_MANAGER"), List.of("payroll"),
                null, null, null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .contains("ACCESS_FEATURE_DISABLED");
    }

    @Test
    void validateRequiresAckReasonWhenNewRisksAreIntroduced() {
        var withoutBaseline = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("PAYROLL_MANAGER"), List.of(),
                null, null, null);
        assertThat(withoutBaseline.valid()).isTrue();

        var withBaseline = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("PAYROLL_MANAGER"), List.of(),
                "target-1", Set.of("VIEWER"), null);
        assertThat(withBaseline.valid()).isFalse();
        assertThat(withBaseline.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .contains("ACCESS_ACK_REASON_REQUIRED");

        var acknowledged = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("PAYROLL_MANAGER"), List.of(),
                "target-1", Set.of("VIEWER"), "Manager needs prepare and approve for month-end");
        assertThat(acknowledged.valid()).isTrue();
    }

    @Test
    void previewReportsRestrictedWhenRouteRoleIsMissing() {
        // HR_REVIEWER grants employees.read but is not part of the employees
        // route-guard matrix, so the page must be reported RESTRICTED with the
        // missing role names listed as missing permissions.
        var preview = service.preview(List.of("HR_REVIEWER"), List.of("employees"));

        var employees = page(preview, "EMPLOYEES");
        assertThat(employees.access()).isEqualTo("RESTRICTED");
        assertThat(employees.missingPermissions()).contains("ADMIN", "HR_MANAGER");
    }

    @Test
    void adminPreviewGrantsFullReviewAccessRegardlessOfMenus() {
        enableAllFeatures();
        var preview = service.preview(List.of("ADMIN"), List.of());

        assertThat(page(preview, "EMPLOYEES").access()).isEqualTo("REVIEW");
        assertThat(page(preview, "PAYROLL").access()).isEqualTo("REVIEW");
        assertThat(page(preview, "USERS").access()).isEqualTo("REVIEW");
        assertThat(preview.pages()).allMatch(item -> "REVIEW".equals(item.access()));
    }

    @Test
    void adminPreviewStillHonorsTenantFeatureFlags() {
        disableFeature("payroll.enabled");
        var preview = service.preview(List.of("ADMIN"), List.of("payroll"));

        assertThat(page(preview, "PAYROLL").access()).isEqualTo("MODULE_UNAVAILABLE");
    }

    @Test
    void validateAssignmentOrThrowRejectsInvalidAssignment() {
        enableAllFeatures();
        assertThatThrownBy(() -> service.validateAssignmentOrThrow(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER"), List.of("payroll"),
                null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("ACCESS_MENU_ROLE_MISMATCH");
    }

    @Test
    void validateAssignmentOrThrowAcceptsValidAssignment() {
        service.validateAssignmentOrThrow(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER"), List.of("dashboard"),
                null, null, null);
    }

    @Test
    void adminValidateBypassesMenuRoleMismatch() {
        enableAllFeatures();
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("ADMIN"), List.of("payroll"),
                null, null, null);
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .doesNotContain("ACCESS_MENU_ROLE_MISMATCH");
    }

    @Test
    void hrManagerDoesNotReceiveOperationsOrAuditPages() {
        assertThat(catalog.permissionsOf("HR_MANAGER")).doesNotContain("operations.read", "audit.read");

        var preview = service.preview(List.of("HR_MANAGER"),
                List.of("operations", "audit-logs", "employees"));
        assertThat(page(preview, "OPERATIONS").access()).isEqualTo("RESTRICTED");
        assertThat(page(preview, "AUDIT_LOGS").access()).isEqualTo("RESTRICTED");
        assertThat(page(preview, "EMPLOYEES").access()).isEqualTo("EDIT");
    }

    @Test
    void catalogExposesApprovalPagesWithMenuIds() {
        var response = service.catalog();

        assertThat(response.pages())
                .extracting(AccessApi.AccessPageResponse::menuId)
                .contains("approvals-my-tasks", "approvals-workflows");
        assertThat(response.pages()).anySatisfy(page -> {
            if ("approvals-my-tasks".equals(page.menuId())) {
                assertThat(page.code()).isEqualTo("PENDING_APPROVALS");
                assertThat(page.route()).isEqualTo("/approvals/my-tasks");
                assertThat(page.titleKey()).isEqualTo("approvals.myTasks");
            }
        });
        assertThat(response.pages()).anySatisfy(page -> {
            if ("approvals-workflows".equals(page.menuId())) {
                assertThat(page.code()).isEqualTo("WORKFLOW_DEFINITIONS");
                assertThat(page.route()).isEqualTo("/approvals/definitions");
                assertThat(page.titleKey()).isEqualTo("approvals.workflows");
                assertThat(page.roles()).contains("ADMIN", "SUPER_ADMIN");
            }
        });
    }

    @Test
    void validateAcceptsApprovalMenusForApprovalRoles() {
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("WORKFORCE_MANAGER"),
                List.of("approvals-my-tasks", "approvals-workflows"), null, null, null);
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .doesNotContain("ACCESS_UNKNOWN_MENU");
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .contains("ACCESS_MENU_ROLE_MISMATCH");
    }

    @Test
    void workflowDefinitionsMenuRequiresAdminRole() {
        var result = service.validateAssignment(
                Set.of("ADMIN"), "actor-1", List.of("VIEWER"), List.of("approvals-workflows"),
                null, null, null);
        assertThat(result.errors()).extracting(AccessApi.AccessValidateErrorResponse::code)
                .contains("ACCESS_MENU_ROLE_MISMATCH");
    }

    @Test
    void approvalRolesPreviewPendingApprovalsAsDecide() {
        var preview = service.preview(List.of("WORKFORCE_MANAGER"),
                List.of("approvals-my-tasks", "approvals-workflows"));

        var pending = page(preview, "PENDING_APPROVALS");
        assertThat(pending.access()).isEqualTo("DECIDE");
        assertThat(pending.grantedActions()).contains("DECIDE");

        var definitions = page(preview, "WORKFLOW_DEFINITIONS");
        assertThat(definitions.access()).isEqualTo("RESTRICTED");
    }

    @Test
    void adminPreviewIncludesApprovalPagesAsReview() {
        var preview = service.preview(List.of("ADMIN"), List.of());

        assertThat(page(preview, "PENDING_APPROVALS").access()).isEqualTo("REVIEW");
        assertThat(page(preview, "WORKFLOW_DEFINITIONS").access()).isEqualTo("REVIEW");
    }

    private AccessApi.EffectivePageAccessResponse page(AccessApi.AccessPreviewResponse preview, String code) {
        return preview.pages().stream()
                .filter(item -> item.pageCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing page " + code));
    }
}
