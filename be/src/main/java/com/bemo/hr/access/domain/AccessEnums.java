package com.bemo.hr.access.domain;

/**
 * Enumerations describing the role-to-page access guidance catalog.
 *
 * <p>These values are the only enum-like constants the access preview and validation
 * logic depends on; the actual role/permission/page definitions live in
 * {@link AccessCatalog}, which is the single source of truth shared with the
 * frontend through {@code GET /api/v1/access/catalog}.
 */
public final class AccessEnums {

    private AccessEnums() {
    }

    /**
     * Business sensitivity of a role or a permission. Used to flag combinations
     * that require an explicit second reviewer or a confirmation before saving.
     */
    public enum AccessSensitivity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Broad role nature used to group role cards in the Add/Edit User screen.
     */
    public enum RoleKind {
        READ_ONLY, OPERATIONAL, APPROVAL, FINANCE, ADMINISTRATION
    }

    /**
     * Severity of a segregation-of-duties rule. {@code BLOCK} rules reject the
     * assignment, {@code WARNING} rules only require the administrator to
     * acknowledge the risk.
     */
    public enum ConflictSeverity {
        WARNING, BLOCK
    }

    /**
     * Effective page access level computed by the preview. Ordering is
     * significant: reverse &gt; post &gt; approve &gt; review &gt; manage &gt;
     * edit &gt; create &gt; view &gt; none.
     */
    public enum AccessLevel {
        NONE, VIEW, CREATE, EDIT, MANAGE, REVIEW, APPROVE, POST, REVERSE,
        RESTRICTED, HIDDEN, MODULE_UNAVAILABLE
    }
}
