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
              and (:search is null or lower(a.entityId) like lower(concat('%', :search, '%'))
                   or lower(a.detailsJson) like lower(concat('%', :search, '%'))
                   or lower(a.action) like lower(concat('%', :search, '%'))
                   or lower(a.entityType) like lower(concat('%', :search, '%'))
                   or lower(a.username) like lower(concat('%', :search, '%')))
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
