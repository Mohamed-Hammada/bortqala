package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessTemplateApi.MenuOptionResponse;
import com.bemo.hr.access.api.AccessTemplateApi.RoleTemplateResponse;
import com.bemo.hr.access.domain.AccessCatalog;
import com.bemo.hr.access.domain.AccessDefs.AccessPageDef;
import com.bemo.hr.access.domain.PolicyGroupPermission;
import com.bemo.hr.access.domain.SecurityPermission;
import com.bemo.hr.access.domain.UserRoleTemplate;
import com.bemo.hr.access.domain.UserRoleTemplateRepository;
import com.bemo.hr.access.infrastructure.PolicyGroupPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPolicyGroupRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.shared.security.TenantFeatureService;
import com.bemo.hr.tenant.application.TenantSetupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * WP-10: server-side menu catalog and vertical job role templates.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class UserRoleTemplateService {

    /** Feature keys mapped to the verticals where the feature defaults ON. */
    private static final Map<String, Set<String>> FEATURE_VERTICALS = Map.of(
            "sales.enabled", Set.of("GENERAL", "MEDICAL", "RETAIL", "MANUFACTURING", "SERVICES"),
            "quality.enabled", Set.of("GENERAL", "MEDICAL", "CIVIL", "MANUFACTURING"),
            "manufacturing.enabled", Set.of("GENERAL", "MANUFACTURING"),
            "workforce.contractorAccounts.enabled", Set.of("GENERAL", "CIVIL"));

    private static final Set<String> ALL_VERTICALS = Set.of(
            "GENERAL", "MEDICAL", "CIVIL", "RETAIL", "MANUFACTURING", "SERVICES");

    private static final int MAX_SUGGESTED_GROUPS = 3;

    private final AccessCatalog accessCatalog;
    private final UserRoleTemplateRepository templateRepository;
    private final SecurityPolicyGroupRepository policyGroupRepository;
    private final PolicyGroupPermissionRepository groupPermissionRepository;
    private final SecurityPermissionRepository permissionRepository;
    private final TenantSetupService tenantSetupService;
    private final TenantFeatureService featureService;

    public UserRoleTemplateService(AccessCatalog accessCatalog,
                                   UserRoleTemplateRepository templateRepository,
                                   SecurityPolicyGroupRepository policyGroupRepository,
                                   PolicyGroupPermissionRepository groupPermissionRepository,
                                   SecurityPermissionRepository permissionRepository,
                                   TenantSetupService tenantSetupService,
                                   TenantFeatureService featureService) {
        this.accessCatalog = accessCatalog;
        this.templateRepository = templateRepository;
        this.policyGroupRepository = policyGroupRepository;
        this.groupPermissionRepository = groupPermissionRepository;
        this.permissionRepository = permissionRepository;
        this.tenantSetupService = tenantSetupService;
        this.featureService = featureService;
    }

    public List<MenuOptionResponse> menuOptions() {
        String appId = TenantContext.require();
        List<MenuOptionResponse> result = new ArrayList<>();
        for (AccessPageDef page : accessCatalog.pages()) {
            String feature = page.requiredFeature();
            boolean enabled = feature == null || featureService.isEnabled(appId, feature);
            result.add(new MenuOptionResponse(
                    page.menuId(),
                    page.titleKey(),
                    page.module(),
                    verticalTagsFor(feature),
                    enabled));
        }
        return List.copyOf(result);
    }

    public List<RoleTemplateResponse> roleTemplates(String requestedVertical) {
        String appId = TenantContext.require();
        Collection<String> verticals = requestedVertical != null && !requestedVertical.isBlank()
                ? appendGeneral(List.of(normalizeVertical(requestedVertical)))
                : defaultVerticals(appId);

        Map<String, UserRoleTemplate> byCode = new LinkedHashMap<>();
        for (UserRoleTemplate template : templateRepository.findForTenant(appId, verticals)) {
            byCode.put(template.getCode(), template);
        }

        Map<String, Set<String>> permissionKeysByGroup = loadPermissionKeysByGroup(appId);

        return byCode.values().stream()
                .map(template -> toResponse(template, permissionKeysByGroup))
                .toList();
    }

    private RoleTemplateResponse toResponse(UserRoleTemplate template,
                                            Map<String, Set<String>> permissionKeysByGroup) {
        List<String> prefixes = splitCsv(template.getPermissionPrefixes());
        return new RoleTemplateResponse(
                template.getCode(),
                template.getNameKey(),
                template.getVertical(),
                splitCsv(template.getMenuIds()),
                prefixes,
                suggestGroups(prefixes, permissionKeysByGroup),
                template.getSortOrder());
    }

    /**
     * Ranks tenant policy groups by how many template permission prefixes match
     * their granted permission keys; returns the best matches in stable order.
     */
    private List<String> suggestGroups(List<String> prefixes,
                                       Map<String, Set<String>> permissionKeysByGroup) {
        if (prefixes.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : permissionKeysByGroup.entrySet()) {
            int score = 0;
            for (String key : entry.getValue()) {
                for (String prefix : prefixes) {
                    if (key.startsWith(prefix)) {
                        score++;
                        break;
                    }
                }
            }
            if (score > 0) {
                scored.add(Map.entry(entry.getKey(), score));
            }
        }
        scored.sort((a, b) -> {
            int byScore = Integer.compare(b.getValue(), a.getValue());
            return byScore != 0 ? byScore : a.getKey().compareTo(b.getKey());
        });
        return scored.stream()
                .limit(MAX_SUGGESTED_GROUPS)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Maps each tenant policy group id to the set of granted permission keys. */
    private Map<String, Set<String>> loadPermissionKeysByGroup(String appId) {
        var groups = policyGroupRepository.findAllByAppIdOrderByGroupNameAsc(appId);
        if (groups.isEmpty()) {
            return Map.of();
        }
        List<String> groupIds = groups.stream().map(g -> g.getId()).toList();
        List<PolicyGroupPermission> grants = groupPermissionRepository.findByPolicyGroupIdIn(groupIds);
        if (grants.isEmpty()) {
            return Map.of();
        }
        Set<String> permissionIds = new HashSet<>();
        for (PolicyGroupPermission grant : grants) {
            permissionIds.add(grant.getPermissionId());
        }
        Map<String, String> keyById = new HashMap<>();
        for (SecurityPermission permission : permissionRepository.findByIdIn(permissionIds)) {
            keyById.put(permission.getId(), permission.getPermissionKey());
        }

        Map<String, Set<String>> result = new HashMap<>();
        for (PolicyGroupPermission grant : grants) {
            String key = keyById.get(grant.getPermissionId());
            if (key != null) {
                result.computeIfAbsent(grant.getPolicyGroupId(), ignored -> new HashSet<>()).add(key);
            }
        }
        return result;
    }

    private Collection<String> defaultVerticals(String appId) {
        try {
            var setup = tenantSetupService.getVerticalSetup();
            return appendGeneral(List.of(setup.vertical().name()));
        } catch (RuntimeException ex) {
            log.warn("Falling back to GENERAL vertical templates: {}", ex.getMessage());
            return List.of("GENERAL");
        }
    }

    private Collection<String> appendGeneral(Collection<String> verticals) {
        Set<String> merged = new HashSet<>(verticals);
        merged.add("GENERAL");
        return merged;
    }

    private String normalizeVertical(String value) {
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!ALL_VERTICALS.contains(normalized)) {
            throw new IllegalArgumentException("UNKNOWN_VERTICAL");
        }
        return normalized;
    }

    public static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static List<String> verticalTagsFor(String requiredFeature) {
        if (requiredFeature == null || !FEATURE_VERTICALS.containsKey(requiredFeature)) {
            return List.copyOf(ALL_VERTICALS);
        }
        return FEATURE_VERTICALS.get(requiredFeature).stream().sorted().toList();
    }
}
