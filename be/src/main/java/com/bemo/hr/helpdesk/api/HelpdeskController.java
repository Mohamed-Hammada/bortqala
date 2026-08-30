package com.bemo.hr.helpdesk.api;

import com.bemo.hr.helpdesk.application.HelpdeskService;
import com.bemo.hr.helpdesk.domain.HelpdeskCategory;
import com.bemo.hr.helpdesk.domain.Ticket;
import com.bemo.hr.helpdesk.domain.TicketMessage;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/helpdesk")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class HelpdeskController {

    private final HelpdeskService service;

    private String resolveAppId(Authentication auth) {
        if (auth.getDetails() instanceof org.springframework.security.oauth2.jwt.Jwt jwt)
            return jwt.getClaimAsString("appId");
        return TenantContext.require();
    }

    private String resolveUserId(Authentication auth) { return auth.getName(); }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    public HelpdeskApi.CategoryResponse createCategory(
            @Valid @RequestBody HelpdeskApi.CreateCategoryPayload p, Authentication auth) {
        String appId = resolveAppId(auth);
        HelpdeskCategory c = service.createCategory(appId, p.nameAr(), p.nameEn(),
                p.slaFirstResponseHours(), p.slaResolutionHours());
        return toCategoryResp(c);
    }

    @GetMapping("/categories")
    public List<HelpdeskApi.CategoryResponse> listCategories(Authentication auth) {
        return service.listCategories(resolveAppId(auth)).stream()
                .map(this::toCategoryResp).toList();
    }

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public HelpdeskApi.TicketResponse createTicket(
            @Valid @RequestBody HelpdeskApi.CreateTicketPayload p, Authentication auth) {
        String appId = resolveAppId(auth);
        Ticket.Priority pri = Ticket.Priority.valueOf(
                p.priority() != null ? p.priority().toUpperCase() : "NORMAL");
        Ticket t = service.createTicket(appId, resolveUserId(auth), p.categoryId(),
                p.title(), p.description(), pri);
        return toTicketResp(t);
    }

    @GetMapping("/tickets")
    public HelpdeskApi.TicketListResponse listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigneeUserId,
            Authentication auth) {
        String appId = resolveAppId(auth);
        String uid = resolveUserId(auth);
        List<Ticket> tickets = service.listTickets(appId, status, assigneeUserId);
        return new HelpdeskApi.TicketListResponse(
                tickets.stream().map(this::toTicketResp).toList(),
                service.countOpenTickets(appId),
                service.countMyOpenTickets(appId, uid));
    }

    @GetMapping("/tickets/{id}")
    public HelpdeskApi.TicketResponse getTicket(@PathVariable String id, Authentication auth) {
        return toTicketResp(service.getTicket(resolveAppId(auth), id));
    }

    @PostMapping("/tickets/{id}/assign")
    public void assignTicket(@PathVariable String id,
                             @Valid @RequestBody HelpdeskApi.AssignPayload p,
                             Authentication auth) {
        service.assignTicket(resolveAppId(auth), id, p.assigneeUserId());
    }

    @PostMapping("/tickets/{id}/transition")
    public void transitionTicket(@PathVariable String id,
                                 @Valid @RequestBody HelpdeskApi.TransitionPayload p,
                                 Authentication auth) {
        service.transitionTicket(resolveAppId(auth), id,
                Ticket.Status.valueOf(p.status().toUpperCase()));
    }

    @PostMapping("/tickets/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public HelpdeskApi.MessageResponse addMessage(
            @PathVariable String id,
            @Valid @RequestBody HelpdeskApi.AddMessagePayload p,
            Authentication auth) {
        String appId = resolveAppId(auth);
        TicketMessage msg = service.addMessage(appId, id, resolveUserId(auth),
                p.body(), p.internalNote());
        return toMessageResp(msg);
    }

    @GetMapping("/tickets/{id}/messages")
    public List<HelpdeskApi.MessageResponse> listMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean includeInternal,
            Authentication auth) {
        return service.listMessages(resolveAppId(auth), id, includeInternal).stream()
                .map(this::toMessageResp).toList();
    }

    private HelpdeskApi.CategoryResponse toCategoryResp(HelpdeskCategory c) {
        return new HelpdeskApi.CategoryResponse(c.getId(), c.getNameAr(), c.getNameEn(),
                c.getSlaFirstResponseHours(), c.getSlaResolutionHours(), c.isActive(), c.getVersion());
    }

    private HelpdeskApi.TicketResponse toTicketResp(Ticket t) {
        return new HelpdeskApi.TicketResponse(t.getId(), t.getTicketNo(), t.getRequesterUserId(),
                t.getCategoryId(), t.getTitle(), t.getDescription(),
                t.getPriorityStr(), t.getStatusStr(), t.getAssigneeUserId(),
                t.getFirstResponseAt() != null ? t.getFirstResponseAt().toEpochMilli() : null,
                t.getResolvedAt() != null ? t.getResolvedAt().toEpochMilli() : null,
                t.getDueFirstResponse() != null ? t.getDueFirstResponse().toEpochMilli() : null,
                t.getDueResolution() != null ? t.getDueResolution().toEpochMilli() : null,
                t.isSlaBreachFirstResponse(), t.isSlaBreachResolution(),
                t.getCreatedAt(), t.getVersion());
    }

    private HelpdeskApi.MessageResponse toMessageResp(TicketMessage m) {
        return new HelpdeskApi.MessageResponse(m.getId(), m.getTicketId(), m.getAuthorUserId(),
                m.getBody(), m.isInternalNote(), m.getAttachmentName(), m.getCreatedAt());
    }
}
