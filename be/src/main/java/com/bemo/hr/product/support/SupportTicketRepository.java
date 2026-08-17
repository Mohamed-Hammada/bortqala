package com.bemo.hr.product.support;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    Optional<SupportTicket> findByOperationId(String operationId);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    long countByStatusNotInAndPriorityIn(Collection<String> statuses, Collection<String> priorities);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SupportTicket t where t.id=:id")
    Optional<SupportTicket> findByIdForUpdate(String id);
}
