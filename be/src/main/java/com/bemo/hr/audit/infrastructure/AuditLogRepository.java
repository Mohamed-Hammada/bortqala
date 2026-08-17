package com.bemo.hr.audit.infrastructure;

import com.bemo.hr.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByOccurredAtDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByUsernameOrderByOccurredAtDesc(String username, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndUsernameOrderByOccurredAtDesc(String entityType, String username, Pageable pageable);

    @Query("""
            select a from AuditLog a
            where (:entityType is null or a.entityType = :entityType)
              and (:action is null or a.action = :action)
              and (:username is null or a.username = :username)
              and (:from is null or a.occurredAt >= :from)
              and (:to is null or a.occurredAt <= :to)
              and (cast(:search as string) is null or lower(cast(a.entityId as string)) like concat('%', lower(cast(:search as string)), '%')
                   or lower(cast(a.detailsJson as string)) like concat('%', lower(cast(:search as string)), '%')
                   or lower(cast(a.action as string)) like concat('%', lower(cast(:search as string)), '%')
                   or lower(cast(a.entityType as string)) like concat('%', lower(cast(:search as string)), '%')
                   or lower(cast(a.username as string)) like concat('%', lower(cast(:search as string)), '%'))
            order by a.occurredAt desc
            """)
    Page<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("username") String username,
            @Param("search") String search,
            @Param("from") Long from,
            @Param("to") Long to,
            Pageable pageable);
}
// BORTQALA_RUNTIME_20260816_V2_AUDIT_SEARCH_TEXT_CAST
