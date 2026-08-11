package com.bemo.hr.shared.transition;

import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.audit.application.AuditService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class DocumentTransitionService {

    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public DocumentTransitionService(IdempotencyService idempotencyService, AuditService auditService) {
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    public <T> TransitionResult<T> transition(
            String documentType,
            String documentId,
            String currentState,
            String targetState,
            Set<String> allowedFromStates,
            long currentVersion,
            TransitionCommand command,
            Supplier<T> stateUpdater,
            Function<T, String> responseSerializer,
            Function<String, T> responseDeserializer
    ) {
        if (!allowedFromStates.contains(currentState)) {
            throw new IllegalDocumentTransitionException(documentType, documentId, currentState, targetState);
        }

        if (command.expectedVersion() != currentVersion) {
            throw new StaleStateConflictException(documentType, documentId, command.expectedVersion(), currentVersion);
        }

        String requestHash = IdempotencyService.hash(
                documentType + ":" + documentId + ":" + currentState + "->" + targetState + ":" + command.expectedVersion()
        );

        T updatedPayload = idempotencyService.execute(
                "TRANSITION_" + documentType,
                command.operationId().toString(),
                requestHash,
                () -> {
                    T payload = stateUpdater.get();
                    auditService.record(
                            "DOCUMENT_TRANSITION",
                            documentType,
                            documentId,
                            "SYSTEM",
                            String.format("Transitioned from %s to %s. Reason: %s", currentState, targetState, command.reason()),
                            "127.0.0.1"
                    );
                    return payload;
                },
                responseSerializer,
                responseDeserializer
        );

        return new TransitionResult<>(
                documentType,
                documentId,
                currentState,
                targetState,
                currentVersion + 1,
                command.operationId(),
                false,
                Instant.now(),
                updatedPayload
        );
    }
}
