package com.bemo.hr.helpdesk.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByAppIdOrderByTicketNoDesc(String appId);
    List<Ticket> findByAppIdAndStatusOrderByTicketNoDesc(String appId, String status);
    List<Ticket> findByAppIdAndAssigneeUserIdOrderByTicketNoDesc(String appId, String assigneeUserId);
    long countByAppIdAndStatus(String appId, String status);
    long countByAppIdAndAssigneeUserIdAndStatusIn(String appId, String assigneeUserId, List<String> statuses);
    Optional<Ticket> findByAppIdAndTicketNo(String appId, long ticketNo);
    @Query("SELECT COALESCE(MAX(t.ticketNo),0) FROM Ticket t WHERE t.appId = :appId")
    long maxTicketNo(@Param("appId") String appId);
}
