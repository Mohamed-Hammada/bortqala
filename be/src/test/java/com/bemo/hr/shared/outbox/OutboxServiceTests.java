package com.bemo.hr.shared.outbox;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTests {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxService service;

    @BeforeEach
    void setUp() {
        service = new OutboxService(outboxEventRepository);
    }

    @Test
    void publishEvent_savesPendingEvent() {
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxEvent event = service.publishEvent("INVOICE_ISSUED", "CustomerInvoice", "inv-001", "{\"total\":1500}");

        assertNotNull(event);
        assertEquals("INVOICE_ISSUED", event.getEventType());
        assertEquals("PENDING", event.getStatus());
        assertEquals(0, event.getRetryCount());
    }

    @Test
    void markEventPublished_updatesStatus() {
        OutboxEvent event = new OutboxEvent("INVOICE_ISSUED", "CustomerInvoice", "inv-001", "{}");
        when(outboxEventRepository.findById("evt-1")).thenReturn(Optional.of(event));

        service.markEventPublished("evt-1");

        assertEquals("PUBLISHED", event.getStatus());
        assertNotNull(event.getProcessedAt());
        assertNull(event.getLastError());
    }

    @Test
    void markEventFailed_incrementsRetryAndSetsDeadLetterWhenExceeded() {
        OutboxEvent event = new OutboxEvent("ETA_SUBMIT", "EtaInvoice", "eta-001", "{}");
        when(outboxEventRepository.findById("evt-1")).thenReturn(Optional.of(event));

        for (int i = 1; i <= 4; i++) {
            service.markEventFailed("evt-1", "Timeout " + i);
            assertEquals("FAILED", event.getStatus());
            assertEquals(i, event.getRetryCount());
        }

        // 5th attempt -> DEAD_LETTER
        service.markEventFailed("evt-1", "Fatal connection failure");
        assertEquals("DEAD_LETTER", event.getStatus());
        assertEquals(5, event.getRetryCount());
    }

    @Test
    void retryEvent_resetsToPending() {
        OutboxEvent event = new OutboxEvent("ETA_SUBMIT", "EtaInvoice", "eta-001", "{}");
        event.markFailed("Error");
        when(outboxEventRepository.findById("evt-1")).thenReturn(Optional.of(event));

        service.retryEvent("evt-1");

        assertEquals("PENDING", event.getStatus());
        assertNull(event.getLastError());
    }

    @Test
    void retryEvent_alreadyPublished_throwsException() {
        OutboxEvent event = new OutboxEvent("ETA_SUBMIT", "EtaInvoice", "eta-001", "{}");
        event.markPublished();
        when(outboxEventRepository.findById("evt-1")).thenReturn(Optional.of(event));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                service.retryEvent("evt-1"));

        assertEquals("OUTBOX_EVENT_ALREADY_PUBLISHED", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
