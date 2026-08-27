package com.bemo.hr.helpdesk.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, String> {
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(String ticketId);
    List<TicketMessage> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(String ticketId);
    long countByTicketIdAndInternalNoteFalse(String ticketId);
}
