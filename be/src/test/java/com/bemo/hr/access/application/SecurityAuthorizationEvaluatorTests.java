package com.bemo.hr.access.application;

import com.bemo.hr.access.api.AccessPolicyApi.UserEffectivePermissionsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityAuthorizationEvaluatorTests {

    private final PolicyGroupService policyGroupService = mock(PolicyGroupService.class);
    private SecurityAuthorizationEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new SecurityAuthorizationEvaluator(policyGroupService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasPermission_adminRole_returnsTrueForAnyPermission() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(evaluator.hasPermission("finance:journal:post")).isTrue();
        assertThat(evaluator.hasPermission("contracting:claim:approve")).isTrue();
    }

    @Test
    void hasPermission_customUserWithPermission_returnsTrue() {
        var auth = new UsernamePasswordAuthenticationToken(
                "accountant",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_ACCOUNTANT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(policyGroupService.getEffectivePermissions("accountant")).thenReturn(
                new UserEffectivePermissionsResponse("u1", "accountant", false,
                        Set.of("finance:journal:post", "finance:journal:read"), Set.of(), Set.of())
        );

        assertThat(evaluator.hasPermission("finance:journal:post")).isTrue();
        assertThat(evaluator.hasPermission("contracting:claim:approve")).isFalse();
    }

    @Test
    void hasBranchAccess_scopedBranch_evaluatesProperly() {
        var auth = new UsernamePasswordAuthenticationToken(
                "storekeeper",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_VIEWER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(policyGroupService.getEffectivePermissions("storekeeper")).thenReturn(
                new UserEffectivePermissionsResponse("u2", "storekeeper", false,
                        Set.of("inventory:stock:read"), Set.of("branch-cairo"), Set.of())
        );

        assertThat(evaluator.hasBranchAccess("branch-cairo")).isTrue();
        assertThat(evaluator.hasBranchAccess("branch-alex")).isFalse();
    }

    @Test
    void hasCostCenterAccess_unrestricted_returnsTrue() {
        var auth = new UsernamePasswordAuthenticationToken(
                "manager",
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_VIEWER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(policyGroupService.getEffectivePermissions("manager")).thenReturn(
                new UserEffectivePermissionsResponse("u3", "manager", false,
                        Set.of("finance:journal:read"), Set.of(), Set.of())
        );

        assertThat(evaluator.hasCostCenterAccess("cc-any")).isTrue();
    }
}
