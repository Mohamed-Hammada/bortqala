package com.bemo.hr.shared.transition;

import java.time.Instant;
import java.util.UUID;

public record TransitionResult<T>(
        String documentType,
        String documentId,
        String fromState,
        String toState,
        long newVersion,
        UUID operationId,
        boolean replayed,
        Instant transitionedAt,
        T payload
) {
}
