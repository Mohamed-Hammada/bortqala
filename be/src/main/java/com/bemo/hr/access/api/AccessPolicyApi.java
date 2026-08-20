package com.bemo.hr.access.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public final class AccessPolicyApi {

    private AccessPolicyApi() {
    }

    public record PermissionDto(
            String id,
            String permissionKey,
            String module,
            String submodule,
            String descriptionKey,
            String action,
            boolean isSystem
    ) {}

    public record ModulePermissionTreeDto(
            String module,
            List<PermissionDto> permissions
    ) {}

    public record PolicyCatalogResponse(
            List<ModulePermissionTreeDto> modules,
            int totalPermissions
    ) {}

    public record PolicyGroupSummaryDto(
            String id,
            String groupName,
            String description,
            boolean isSystem,
            int permissionsCount,
            int assignedUsersCount,
            long createdAt,
            long updatedAt,
            long version
    ) {}

    public record PolicyGroupDetailDto(
            String id,
            String groupName,
            String description,
            boolean isSystem,
            List<String> permissionKeys,
            int assignedUsersCount,
            long createdAt,
            long updatedAt,
            long version
    ) {}

    public record CreatePolicyGroupRequest(
            @NotBlank(message = "groupName is required")
            String groupName,
            String description,
            List<String> permissionKeys
    ) {}

    public record UpdatePolicyGroupRequest(
            @NotBlank(message = "groupName is required")
            String groupName,
            String description,
            List<String> permissionKeys,
            Long version
    ) {}

    public record UserPolicyAssignmentItem(
            @NotBlank(message = "policyGroupId is required")
            String policyGroupId,
            String scopeBranchId,
            String scopeCostCenterId
    ) {}

    public record AssignUserPoliciesRequest(
            @NotNull
            List<@Valid UserPolicyAssignmentItem> assignments
    ) {}

    public record UserPolicyAssignmentDto(
            String id,
            String userId,
            String policyGroupId,
            String policyGroupName,
            String scopeBranchId,
            String scopeCostCenterId,
            long assignedAt
    ) {}

    public record UserEffectivePermissionsResponse(
            String userId,
            String username,
            boolean isAdmin,
            Set<String> permissions,
            Set<String> branchScopes,
            Set<String> costCenterScopes
    ) {}
}
