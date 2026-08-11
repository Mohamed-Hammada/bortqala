package com.bemo.hr.shared.transition;

import com.bemo.hr.shared.idempotency.application.IdempotencyService;
import com.bemo.hr.audit.application.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DocumentTransitionServiceTests {

    private IdempotencyService idempotencyService;
    private AuditService auditService;
    private DocumentTransitionService transitionService;

    @BeforeEach
    void setUp() {
        idempotencyService = mock(IdempotencyService.class);
        auditService = mock(AuditService.class);
        transitionService = new DocumentTransitionService(idempotencyService, auditService);
    }

    @Test
    void executeTransitionSuccessfully() {
        UUID opId = UUID.randomUUID();
        TransitionCommand command = new TransitionCommand(opId, 1L, "Approve request");

        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier = invocation.getArgument(3);
                    return supplier.get();
                });

        TransitionResult<String> result = transitionService.transition(
                "LABOR_REQUEST",
                "LR-100",
                "SUBMITTED",
                "APPROVED",
                Set.of("SUBMITTED", "PENDING_APPROVAL"),
                1L,
                command,
                () -> "LR-100-APPROVED",
                payload -> payload,
                raw -> raw
        );

        assertThat(result.documentType()).isEqualTo("LABOR_REQUEST");
        assertThat(result.documentId()).isEqualTo("LR-100");
        assertThat(result.fromState()).isEqualTo("SUBMITTED");
        assertThat(result.toState()).isEqualTo("APPROVED");
        assertThat(result.newVersion()).isEqualTo(2L);
        assertThat(result.payload()).isEqualTo("LR-100-APPROVED");

        verify(auditService, times(1)).record(eq("DOCUMENT_TRANSITION"), eq("LABOR_REQUEST"), eq("LR-100"), anyString(), anyString(), anyString());
    }

    @Test
    void rejectsInvalidFromState() {
        UUID opId = UUID.randomUUID();
        TransitionCommand command = new TransitionCommand(opId, 1L, "Approve draft");

        assertThatThrownBy(() -> transitionService.transition(
                "LABOR_REQUEST",
                "LR-100",
                "DRAFT",
                "APPROVED",
                Set.of("SUBMITTED", "PENDING_APPROVAL"),
                1L,
                command,
                () -> "LR-100-APPROVED",
                payload -> payload,
                raw -> raw
        )).isInstanceOf(IllegalDocumentTransitionException.class);
    }

    @Test
    void rejectsVersionMismatch() {
        UUID opId = UUID.randomUUID();
        TransitionCommand command = new TransitionCommand(opId, 1L, "Stale edit");

        assertThatThrownBy(() -> transitionService.transition(
                "LABOR_REQUEST",
                "LR-100",
                "SUBMITTED",
                "APPROVED",
                Set.of("SUBMITTED"),
                2L, // actual version is 2, expected was 1
                command,
                () -> "LR-100-APPROVED",
                payload -> payload,
                raw -> raw
        )).isInstanceOf(StaleStateConflictException.class);
    }
}
