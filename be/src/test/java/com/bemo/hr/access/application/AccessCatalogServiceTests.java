package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessApi;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the role-to-page access guidance: catalog integrity, effective
 * access preview, segregation-of-duties evaluation and the authoritative
 * assignment validation used before a user is saved.
 */
class AccessCatalogServiceTests {

    private final AccessCatalog catalog = new AccessCatalog();
    private final AccessCatalogService service = new AccessCatalogService(catalog);

    @Test
    void catalogExposesEveryRolePageAndRule() {
        var response = service.catalog();

        assertThat(response.roles()).hasSize(19);
        assertThat(response.roles()).extracting(AccessApi.AccessRoleResponse::code)
                .contains("SUPER_ADMIN", "ADMIN", "HR_MANAGER", "WORKFORCE_MANAGER", "AUDITOR");
        assertThat(response.pages()).isNotEmpty();
        assertThat(response.pages()).allSatisfy(page ->
                assertThat(page.menuId()).isNotBlank());
        assertThat(response.conflictRules()).hasSize(5);
        assertThat(response.conflictRules()).allSatisfy(rule ->
                assertThat(rule.permissions()).isNotEmpty());
        assertThat(response.sensitivePermissions()).contains("journal.post", "users.manage");
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

    private AccessApi.EffectivePageAccessResponse page(AccessApi.AccessPreviewResponse preview, String code) {
        return preview.pages().stream()
                .filter(item -> item.pageCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing page " + code));
    }
}
