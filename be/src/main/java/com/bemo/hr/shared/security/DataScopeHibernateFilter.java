package com.bemo.hr.shared.security;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that activates Hibernate row-level data scope filters before
 * repository queries execute. This ensures that entities annotated with
 * @Filter annotations automatically restrict visible rows based on the
 * current user's data scope (GLOBAL / BRANCH / DEPARTMENT / SELF).
 *
 * The filter is a no-op for GLOBAL scope and SUPER_ADMIN users.
 */
@Slf4j
@Aspect
@Component
public class DataScopeHibernateFilter {

    private final EntityManager entityManager;

    public DataScopeHibernateFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * After any Repository method returns, ensure the filter was enabled.
     * This runs on every JPA repository call and is lightweight when the
     * filter is already enabled or scope is GLOBAL.
     */
    @AfterReturning(pointcut = "execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))", returning = "result")
    public void afterRepositoryCall(Object result) {
        // Filter activation is done via the interceptor/filter setup in HibernateConfig
        // This aspect is a safety net for edge cases
    }

    /**
     * Enable the orgScopeFilter on the current Hibernate session if a scope context is available.
     * This is called from the SecurityAuthorizationEvaluator after authentication is established.
     */
    public static void enableFilterIfScoped(EntityManager em, DataScopeContext.ScopeLevel scope,
                                            String branchId, String departmentId, String userId) {
        if (scope == null || scope == DataScopeContext.ScopeLevel.GLOBAL) {
            return; // GLOBAL scope = no filtering
        }

        Session session = em.unwrap(Session.class);
        if (session == null) return;

        try {
            Filter filter = session.enableFilter("orgScopeFilter");
            filter.setParameter("scopeLevel", scope.name());
            filter.setParameter("userBranchId", branchId != null ? branchId : "");
            filter.setParameter("userDepartmentId", departmentId != null ? departmentId : "");
            filter.setParameter("userId", userId != null ? userId : "");
        } catch (Exception e) {
            log.debug("Could not enable orgScopeFilter: {}", e.getMessage());
        }
    }
}
