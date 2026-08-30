package com.bemo.hr.access.api;

import com.bemo.hr.access.api.AccessPolicyApi.*;
import com.bemo.hr.access.application.PolicyGroupService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/access")
public class AccessPolicyController {

    private final PolicyGroupService policyGroupService;

    public AccessPolicyController(PolicyGroupService policyGroupService) {
        this.policyGroupService = policyGroupService;
    }

    @GetMapping("/catalog/permissions")
    @PreAuthorize("isAuthenticated()")
    public PolicyCatalogResponse getCatalog() {
        return policyGroupService.getCatalog();
    }

    @GetMapping("/policy-groups")
    @PreAuthorize("isAuthenticated()")
    public List<PolicyGroupSummaryDto> listPolicyGroups() {
        return policyGroupService.listPolicyGroups();
    }

    @GetMapping("/policy-groups/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public PolicyGroupDetailDto getPolicyGroup(@PathVariable String groupId) {
        return policyGroupService.getPolicyGroup(groupId);
    }

    @PostMapping("/policy-groups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @auth.hasPermission('access:policy:create')")
    public PolicyGroupDetailDto createPolicyGroup(@Valid @RequestBody CreatePolicyGroupRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "system";
        return policyGroupService.createPolicyGroup(request, actor);
    }

    @PutMapping("/policy-groups/{groupId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @auth.hasPermission('access:policy:update')")
    public PolicyGroupDetailDto updatePolicyGroup(@PathVariable String groupId,
                                                  @Valid @RequestBody UpdatePolicyGroupRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "system";
        return policyGroupService.updatePolicyGroup(groupId, request, actor);
    }

    @DeleteMapping("/policy-groups/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @auth.hasPermission('access:policy:delete')")
    public void deletePolicyGroup(@PathVariable String groupId,
                                  @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "system";
        policyGroupService.deletePolicyGroup(groupId, actor);
    }

    @GetMapping("/users/{userId}/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @auth.hasPermission('access:user:read')")
    public List<UserPolicyAssignmentDto> getUserPolicies(@PathVariable String userId) {
        return policyGroupService.getUserPolicyAssignments(userId);
    }

    @PostMapping("/users/{userId}/policies")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or @auth.hasPermission('access:user_policy:assign')")
    public List<UserPolicyAssignmentDto> assignUserPolicies(@PathVariable String userId,
                                                            @Valid @RequestBody AssignUserPoliciesRequest request,
                                                            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "system";
        return policyGroupService.assignUserPolicies(userId, request, actor);
    }

    @GetMapping("/me/permissions")
    @PreAuthorize("isAuthenticated()")
    public UserEffectivePermissionsResponse getMyPermissions(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt != null ? jwt.getSubject() : "system";
        return policyGroupService.getEffectivePermissions(username);
    }
}
