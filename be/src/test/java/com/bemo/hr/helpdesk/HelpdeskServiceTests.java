package com.bemo.hr.helpdesk;

import com.bemo.hr.helpdesk.application.HelpdeskService;
import com.bemo.hr.helpdesk.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpdeskServiceTests {

    @Mock private HelpdeskCategoryRepository categoryRepo;
    @Mock private TicketRepository ticketRepo;
    @Mock private TicketMessageRepository messageRepo;
    @InjectMocks private HelpdeskService service;

    private HelpdeskCategory category;

    @BeforeEach
    void setUp() {
        category = new HelpdeskCategory("app1", "الدعم", "Support", 8, 48);
        lenient().when(categoryRepo.findById(any())).thenReturn(Optional.of(category));
        lenient().when(ticketRepo.maxTicketNo(any())).thenReturn(0L);
    }

    @Test
    void createTicket_stampsSlaWithPriorityMultiplier() {
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Ticket t = service.createTicket("app1", "user1", "cat1", "title", "desc", Ticket.Priority.URGENT);
        assertThat(t.getDueFirstResponse()).isNotNull();
        assertThat(t.getDueResolution()).isNotNull();
        assertThat(t.getStatus()).isEqualTo(Ticket.Status.NEW);
    }

    @Test
    void addMessage_nonInternal_setsFirstResponse() {
        Ticket ticket = new Ticket("app1", 1L, "user1", "cat1", "title", "desc", Ticket.Priority.NORMAL);
        when(ticketRepo.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addMessage("app1", ticket.getId(), "agent1", "Hello", false);
        assertThat(ticket.getFirstResponseAt()).isNotNull();
    }

    @Test
    void addMessage_internal_doesNotSetFirstResponse() {
        Ticket ticket = new Ticket("app1", 1L, "user1", "cat1", "title", "desc", Ticket.Priority.NORMAL);
        when(ticketRepo.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(messageRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.addMessage("app1", ticket.getId(), "agent1", "Internal note", true);
        assertThat(ticket.getFirstResponseAt()).isNull();
    }

    @Test
    void transitionTicket_resolved_setsResolvedAt() {
        Ticket ticket = new Ticket("app1", 1L, "user1", "cat1", "title", "desc", Ticket.Priority.NORMAL);
        when(ticketRepo.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.transitionTicket("app1", ticket.getId(), Ticket.Status.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
        assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.RESOLVED);
    }

    @Test
    void checkSlaBreaches_flagsBreachWhenDue() {
        Ticket ticket = new Ticket("app1", 1L, "user1", "cat1", "title", "desc", Ticket.Priority.URGENT);
        ticket.stampSla(java.time.Instant.now().minusSeconds(86400 * 10), 8, 48, Ticket.Priority.URGENT);
        when(ticketRepo.findByAppIdOrderByTicketNoDesc(any())).thenReturn(java.util.List.of(ticket));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.checkSlaBreaches("app1");
        assertThat(ticket.isSlaBreachFirstResponse()).isTrue();
        assertThat(ticket.isSlaBreachResolution()).isTrue();
    }
}
