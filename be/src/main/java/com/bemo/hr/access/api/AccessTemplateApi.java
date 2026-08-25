package com.bemo.hr.access.api;

import java.util.List;

/** WP-10 vertical-aware user creation payloads. */
public final class AccessTemplateApi {

    private AccessTemplateApi() {
    }

    /**
     * One server-side menu catalog entry.
     *
     * @param id           canonical menu id (AccessCatalog menuId)
     * @param labelKey     translation key for the label
     * @param groupKey     workspace/module group key
     * @param verticalTags business verticals where this menu is relevant
     * @param enabled      true when no feature flag disables it on this tenant
     */
    public record MenuOptionResponse(
            String id,
            String labelKey,
            String groupKey,
            List<String> verticalTags,
            boolean enabled) {
    }

    /**
     * One job role template.
     *
     * @param code                    stable template code (e.g. PHARMACIST)
     * @param nameKey                 translation key {@code users.template.<code>}
     * @param vertical                owning vertical (GENERAL rows are shared)
     * @param menuIds                 menus pre-checked when the admin applies the template
     * @param permissionPrefixes      permission prefixes used to rank suggested policy groups
     * @param suggestedPolicyGroupIds tenant policy groups whose permissions overlap the prefixes
     * @param sortOrder               display order
     */
    public record RoleTemplateResponse(
            String code,
            String nameKey,
            String vertical,
            List<String> menuIds,
            List<String> permissionPrefixes,
            List<String> suggestedPolicyGroupIds,
            int sortOrder) {
    }
}
