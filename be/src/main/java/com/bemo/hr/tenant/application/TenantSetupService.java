package com.bemo.hr.tenant.application;

import com.bemo.hr.access.domain.PolicyGroupPermission;
import com.bemo.hr.access.domain.SecurityPermission;
import com.bemo.hr.access.domain.SecurityPolicyGroup;
import com.bemo.hr.access.infrastructure.PolicyGroupPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPermissionRepository;
import com.bemo.hr.access.infrastructure.SecurityPolicyGroupRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.shared.security.*;
import com.bemo.hr.tenant.api.TenantSetupApi.TenantVerticalResponse;
import com.bemo.hr.tenant.domain.BusinessVertical;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class TenantSetupService {

    private final TenantFeatureRepository featureRepository;
    private final TenantFeatureService featureService;
    private final SecurityPolicyGroupRepository policyGroupRepository;
    private final PolicyGroupPermissionRepository groupPermissionRepository;
    private final SecurityPermissionRepository permissionRepository;
    private final AuditService auditService;

    public TenantSetupService(TenantFeatureRepository featureRepository,
                              TenantFeatureService featureService,
                              SecurityPolicyGroupRepository policyGroupRepository,
                              PolicyGroupPermissionRepository groupPermissionRepository,
                              SecurityPermissionRepository permissionRepository,
                              AuditService auditService) {
        this.featureRepository = featureRepository;
        this.featureService = featureService;
        this.policyGroupRepository = policyGroupRepository;
        this.groupPermissionRepository = groupPermissionRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public TenantVerticalResponse getVerticalSetup() {
        String appId = TenantContext.require();
        Set<String> activeFeatures = featureService.getAllEnabled(appId);
        List<SecurityPolicyGroup> groups = policyGroupRepository.findAllByAppIdOrderByGroupNameAsc(appId);
        List<String> groupNames = groups.stream().map(SecurityPolicyGroup::getGroupName).toList();

        BusinessVertical vertical = inferVertical(activeFeatures);
        return new TenantVerticalResponse(appId, vertical, activeFeatures, groupNames);
    }

    public TenantVerticalResponse configureTenantVertical(BusinessVertical vertical, String actorUsername) {
        log.info("Configuring business vertical {} for tenant {}", vertical, TenantContext.require());
        String appId = TenantContext.require();

        Map<String, Boolean> featuresToApply = getFeaturesForVertical(vertical);
        for (Map.Entry<String, Boolean> entry : featuresToApply.entrySet()) {
            TenantFeature feature = new TenantFeature(appId, entry.getKey(), entry.getValue(),
                    "Configured by business vertical setup: " + vertical.name(), actorUsername);
            featureRepository.save(feature);
        }

        List<String> provisioned = provisionDefaultPolicyGroups(appId, vertical);

        auditService.record("BUSINESS_VERTICAL_CONFIGURED", "TENANT", appId, actorUsername,
                "Configured business vertical to " + vertical.name() + " with " + provisioned.size() + " groups", null);

        Set<String> activeFeatures = featureService.getAllEnabled(appId);
        return new TenantVerticalResponse(appId, vertical, activeFeatures, provisioned);
    }

    private Map<String, Boolean> getFeaturesForVertical(BusinessVertical vertical) {
        Map<String, Boolean> map = new HashMap<>();
        // Base features always on
        map.put("finance.enabled", true);
        map.put("payroll.enabled", true);

        switch (vertical) {
            case MEDICAL -> {
                map.put("sales.enabled", true);
                map.put("procurement.enabled", true);
                map.put("quality.enabled", true);
                map.put("manufacturing.enabled", false);
                map.put("workforce.contractorAccounts.enabled", false);
            }
            case CIVIL -> {
                map.put("sales.enabled", false);
                map.put("procurement.enabled", true);
                map.put("quality.enabled", true);
                map.put("manufacturing.enabled", false);
                map.put("workforce.contractorAccounts.enabled", true);
            }
            case RETAIL -> {
                map.put("sales.enabled", true);
                map.put("procurement.enabled", true);
                map.put("quality.enabled", false);
                map.put("manufacturing.enabled", false);
                map.put("workforce.contractorAccounts.enabled", false);
            }
            case MANUFACTURING -> {
                map.put("sales.enabled", true);
                map.put("procurement.enabled", true);
                map.put("quality.enabled", true);
                map.put("manufacturing.enabled", true);
                map.put("workforce.contractorAccounts.enabled", false);
            }
            case SERVICES -> {
                map.put("sales.enabled", true);
                map.put("procurement.enabled", false);
                map.put("quality.enabled", false);
                map.put("manufacturing.enabled", false);
                map.put("workforce.contractorAccounts.enabled", false);
            }
            case GENERAL -> {
                map.put("sales.enabled", true);
                map.put("procurement.enabled", true);
                map.put("quality.enabled", true);
                map.put("manufacturing.enabled", true);
                map.put("workforce.contractorAccounts.enabled", true);
            }
        }
        return map;
    }

    private List<String> provisionDefaultPolicyGroups(String appId, BusinessVertical vertical) {
        List<String> createdNames = new ArrayList<>();
        List<DefaultGroupSpec> specs = getDefaultSpecsForVertical(vertical);

        for (DefaultGroupSpec spec : specs) {
            if (!policyGroupRepository.existsByAppIdAndGroupNameIgnoreCase(appId, spec.name())) {
                SecurityPolicyGroup group = new SecurityPolicyGroup(spec.name(), spec.description(), false);
                SecurityPolicyGroup saved = policyGroupRepository.save(group);

                List<SecurityPermission> matchingPerms = findMatchingPermissions(spec.prefixes());
                List<PolicyGroupPermission> links = matchingPerms.stream()
                        .map(p -> new PolicyGroupPermission(saved.getId(), p.getId()))
                        .toList();
                groupPermissionRepository.saveAll(links);
                createdNames.add(saved.getGroupName());
            }
        }
        return createdNames;
    }

    private List<SecurityPermission> findMatchingPermissions(List<String> prefixes) {
        List<SecurityPermission> all = permissionRepository.findAll();
        return all.stream()
                .filter(p -> prefixes.stream().anyMatch(prefix ->
                        p.getPermissionKey().startsWith(prefix) || p.getModule().equalsIgnoreCase(prefix)))
                .toList();
    }

    private record DefaultGroupSpec(String name, String description, List<String> prefixes) {}

    private List<DefaultGroupSpec> getDefaultSpecsForVertical(BusinessVertical vertical) {
        return switch (vertical) {
            case MEDICAL -> List.of(
                    new DefaultGroupSpec("Clinic Administrator", "Full medical administration, billing, and scheduling", List.of("trade", "procurement", "finance", "hr")),
                    new DefaultGroupSpec("Medical Receptionist", "Patient registration, appointment scheduling and front desk", List.of("sales:so:read", "hr:employee:read"))
            );
            case CIVIL -> List.of(
                    new DefaultGroupSpec("Project Manager", "Contracting WBS, project estimating, subcontractor claims", List.of("contracting", "procurement", "finance")),
                    new DefaultGroupSpec("Site Superintendent", "Daily progress reports, site weather logs, workforce verification", List.of("contracting:dpr", "contracting:wbs:read", "hr:attendance"))
            );
            case RETAIL -> List.of(
                    new DefaultGroupSpec("Store Manager", "Retail POS, van sales route, and inventory replenishment", List.of("pos", "trade", "inventory", "finance")),
                    new DefaultGroupSpec("Cashier Lead", "Counter order checkout and shift cash reconciliation", List.of("pos:order", "pos:shift"))
            );
            case MANUFACTURING -> List.of(
                    new DefaultGroupSpec("Plant Supervisor", "BOM engineering, work order execution, and machine downtime", List.of("manufacturing", "inventory", "procurement")),
                    new DefaultGroupSpec("Quality Inspector", "Quality control inspections and non-conformance logs", List.of("manufacturing:quality", "inventory:stock:read"))
            );
            case SERVICES -> List.of(
                    new DefaultGroupSpec("Practice Lead", "Client CRM engagements, professional contracts, and billing", List.of("crm", "finance", "hr")),
                    new DefaultGroupSpec("Project Consultant", "Timesheets, client milestones, and task deliverables", List.of("crm:deal:read", "hr:timesheet"))
            );
            case GENERAL -> List.of(
                    new DefaultGroupSpec("Operations Lead", "General enterprise operations, trade orders, and inventory", List.of("trade", "procurement", "inventory")),
                    new DefaultGroupSpec("Financial Controller", "Financial reporting, general ledger, and compliance", List.of("finance", "compliance"))
            );
        };
    }

    private BusinessVertical inferVertical(Set<String> activeFeatures) {
        if (activeFeatures.contains("manufacturing.enabled") && activeFeatures.contains("workforce.contractorAccounts.enabled")) {
            return BusinessVertical.GENERAL;
        }
        if (activeFeatures.contains("manufacturing.enabled")) {
            return BusinessVertical.MANUFACTURING;
        }
        if (activeFeatures.contains("workforce.contractorAccounts.enabled")) {
            return BusinessVertical.CIVIL;
        }
        return BusinessVertical.GENERAL;
    }
}
