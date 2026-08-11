package com.bemo.hr.shared.transition;

import java.util.UUID;

public record TransitionCommand(
    UUID operationId,
    long expectedVersion,
    String reason
) {
    public TransitionCommand {
        if (operationId == null) {
            throw new IllegalArgumentException("operationId must not be null");
        }
    }
}
