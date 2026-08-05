package com.bemo.hr.access.domain;

import com.bemo.hr.access.domain.AccessEnums.AccessSensitivity;
import com.bemo.hr.access.domain.AccessEnums.ConflictSeverity;
import com.bemo.hr.access.domain.AccessEnums.RoleKind;

import java.util.List;
import java.util.Set;

/**
 * Immutable catalog records. These are domain value objects: they carry no
 * Spring or JPA dependencies and are serialized as-is by the access API.
 */
public final class AccessDefs {

    private AccessDefs() {
    }

    /**
     * A named role with its business translation keys, sensitivity, broad kind,
     * granted permissions, required companion roles and an optional sensitive
     * reason key shown when the role is selected.
     */
    public record AccessRoleDef(String code, String nameKey, AccessSensitivity sensitivity, RoleKind kind,
                                Set<String> permissions, Set<String> dependencies, String sensitiveReasonKey) {
    }

    /**
     * A protected page: its module, route, shell menu id, title key and the
     * permission that unlocks viewing it plus per-action permissions.
     */
    public record AccessPageDef(String code, String module, String route, String menuId, String titleKey,
                                String viewPermission, List<AccessActionDef> actions) {
    }

    /**
     * A single action available on a page, gated by one permission.
     */
    public record AccessActionDef(String code, String permission, boolean sensitive) {
    }

    /**
     * A segregation-of-duties rule: when every permission in the list is granted
     * together, the assignment triggers the rule at the given severity.
     */
    public record AccessConflictRuleDef(String code, List<String> permissions,
                                        ConflictSeverity severity, String reasonKey) {
    }

    /**
     * A business need used by the guided selection mode; maps to the set of
     * permissions the task requires.
     */
    public record AccessNeedDef(String code, String labelKey, Set<String> permissions) {
    }
}
