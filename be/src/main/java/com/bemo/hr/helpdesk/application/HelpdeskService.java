package com.bemo.hr.helpdesk.application;

import com.bemo.hr.helpdesk.domain.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpdeskService {

    private final HelpdeskCategoryRepository categoryRepo;
    private final TicketRepository ticketRepo;
    private final TicketMessageRepository messageRepo;

    @Transactional
    public HelpdeskCategory createCategory(String appId, String nameAr, String nameEn,
                                           int slaFirstResp, int slaResolution) {
        var cat = new HelpdeskCategory(appId, nameAr, nameEn, slaFirstResp, slaResolution);
        return categoryRepo.save(cat);
    }

    @Transactional(readOnly = true)
    public List<HelpdeskCategory> listCategories(String appId) {
        return categoryRepo.findByAppIdOrderByCreatedAtDesc(appId);
    }

    @Transactional
    public Ticket createTicket(String appId, String requesterUserId, String categoryId,
                               String title, String description, Ticket.Priority priority) {
        HelpdeskCategory cat = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new BusinessRuleException("Category not found.",
                        "TICKET_CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND));
        long nextNo = ticketRepo.maxTicketNo(appId) + 1;
        Ticket ticket = new Ticket(appId, nextNo, requesterUserId, categoryId, title, description, priority);
        ticket.stampSla(Instant.now(), cat.getSlaFirstResponseHours(), cat.getSlaResolutionHours(), priority);
        return ticketRepo.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<Ticket> listTickets(String appId, String status, String assigneeUserId) {
        if (status != null) return ticketRepo.findByAppIdAndStatusOrderByTicketNoDesc(appId, status);
        if (assigneeUserId != null) return ticketRepo.findByAppIdAndAssigneeUserIdOrderByTicketNoDesc(appId, assigneeUserId);
        return ticketRepo.findByAppIdOrderByTicketNoDesc(appId);
    }

    @Transactional(readOnly = true)
    public Ticket getTicket(String appId, String ticketId) {
        return ticketRepo.findById(ticketId)
                .filter(t -> t.getAppId().equals(appId))
                .orElseThrow(() -> new BusinessRuleException("Ticket not found.",
                        "TICKET_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void assignTicket(String appId, String ticketId, String assigneeUserId) {
        Ticket t = getTicket(appId, ticketId);
        t.assign(assigneeUserId);
        ticketRepo.save(t);
    }

    @Transactional
    public void transitionTicket(String appId, String ticketId, Ticket.Status newStatus) {
        Ticket t = getTicket(appId, ticketId);
        t.transitionTo(newStatus);
        if (newStatus == Ticket.Status.RESOLVED) t.resolve(Instant.now());
        ticketRepo.save(t);
    }

    @Transactional
    public TicketMessage addMessage(String appId, String ticketId, String authorUserId,
                                    String body, boolean internalNote) {
        Ticket t = getTicket(appId, ticketId);
        TicketMessage msg = new TicketMessage(appId, ticketId, authorUserId, body, internalNote);
        messageRepo.save(msg);
        if (!internalNote && t.getFirstResponseAt() == null) {
            t.markFirstResponse(Instant.now());
            ticketRepo.save(t);
        }
        return msg;
    }

    @Transactional(readOnly = true)
    public List<TicketMessage> listMessages(String appId, String ticketId, boolean includeInternal) {
        getTicket(appId, ticketId);
        if (includeInternal) return messageRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return messageRepo.findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(ticketId);
    }

    public long countOpenTickets(String appId) {
        return ticketRepo.countByAppIdAndStatus(appId, Ticket.Status.NEW.name())
             + ticketRepo.countByAppIdAndStatus(appId, Ticket.Status.OPEN.name());
    }

    public long countMyOpenTickets(String appId, String userId) {
        return ticketRepo.countByAppIdAndAssigneeUserIdAndStatusIn(appId, userId,
                List.of(Ticket.Status.NEW.name(), Ticket.Status.OPEN.name()));
    }

    @Transactional
    public void checkSlaBreaches(String appId) {
        Instant now = Instant.now();
        ticketRepo.findByAppIdOrderByTicketNoDesc(appId).stream()
            .filter(t -> t.getStatus() != Ticket.Status.CLOSED && t.getStatus() != Ticket.Status.RESOLVED)
            .forEach(t -> {
                boolean changed = false;
                if (!t.isSlaBreachFirstResponse() && t.getFirstResponseAt() == null
                        && t.getDueFirstResponse() != null && now.isAfter(t.getDueFirstResponse())) {
                    t.setSlaBreachFirstResponse(true);
                    changed = true;
                }
                if (!t.isSlaBreachResolution() && t.getResolvedAt() == null
                        && t.getDueResolution() != null && now.isAfter(t.getDueResolution())) {
                    t.setSlaBreachResolution(true);
                    changed = true;
                }
                if (changed) ticketRepo.save(t);
            });
    }
}
