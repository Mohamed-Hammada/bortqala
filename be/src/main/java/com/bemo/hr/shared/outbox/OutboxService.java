package com.bemo.hr.shared.outbox;

import com.bemo.hr.shared.domain.BusinessRuleException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public OutboxEvent publishEvent(String eventType, String aggregateType, String aggregateId, String payloadJson) {
        OutboxEvent event = new OutboxEvent(eventType, aggregateType, aggregateId, payloadJson);
        return outboxEventRepository.save(event);
    }

    @Transactional
    public void markEventPublished(String eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessRuleException("Outbox event not found", "OUTBOX_EVENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        event.markPublished();
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markEventFailed(String eventId, String errorMessage) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessRuleException("Outbox event not found", "OUTBOX_EVENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        event.markFailed(errorMessage);
        outboxEventRepository.save(event);
    }

    @Transactional
    public void retryEvent(String eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessRuleException("Outbox event not found", "OUTBOX_EVENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (event.isPublished()) {
            throw new BusinessRuleException("Event already published", "OUTBOX_EVENT_ALREADY_PUBLISHED", HttpStatus.CONFLICT);
        }

        event.retry();
        outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public OutboxApi.OutboxPageResponse listEvents(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<OutboxEvent> eventPage = (status != null && !status.isBlank())
                ? outboxEventRepository.findByStatusOrderByCreatedAtDesc(status.strip().toUpperCase(), pageable)
                : outboxEventRepository.findAll(pageable);

        List<OutboxApi.OutboxEventSummary> summaries = eventPage.getContent().stream()
                .map(OutboxApi.OutboxEventSummary::from)
                .toList();

        return new OutboxApi.OutboxPageResponse(
                summaries,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public OutboxApi.OutboxStatsResponse getStats() {
        long pending = outboxEventRepository.countByStatus("PENDING");
        long published = outboxEventRepository.countByStatus("PUBLISHED");
        long failed = outboxEventRepository.countByStatus("FAILED");
        long deadLetter = outboxEventRepository.countByStatus("DEAD_LETTER");
        return new OutboxApi.OutboxStatsResponse(pending, published, failed, deadLetter);
    }
}
