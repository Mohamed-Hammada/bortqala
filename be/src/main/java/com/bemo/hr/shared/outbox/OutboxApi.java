package com.bemo.hr.shared.outbox;

import java.time.Instant;
import java.util.List;

public final class OutboxApi {

    private OutboxApi() {
    }

    public record OutboxEventSummary(
            String id,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payloadJson,
            String status,
            int retryCount,
            int maxRetries,
            String lastError,
            Instant createdAt,
            Instant processedAt
    ) {
        public static OutboxEventSummary from(OutboxEvent e) {
            return new OutboxEventSummary(
                    e.getId(),
                    e.getEventType(),
                    e.getAggregateType(),
                    e.getAggregateId(),
                    e.getPayloadJson(),
                    e.getStatus(),
                    e.getRetryCount(),
                    e.getMaxRetries(),
                    e.getLastError(),
                    e.getCreatedAt(),
                    e.getProcessedAt()
            );
        }
    }

    public record OutboxStatsResponse(
            long pendingCount,
            long publishedCount,
            long failedCount,
            long deadLetterCount
    ) {}

    public record OutboxPageResponse(
            List<OutboxEventSummary> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
