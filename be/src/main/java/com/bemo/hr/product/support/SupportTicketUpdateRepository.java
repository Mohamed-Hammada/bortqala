package com.bemo.hr.product.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportTicketUpdateRepository extends JpaRepository<SupportTicketUpdate, String> {
    Optional<SupportTicketUpdate> findByOperationId(String operationId);

    List<SupportTicketUpdate> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
