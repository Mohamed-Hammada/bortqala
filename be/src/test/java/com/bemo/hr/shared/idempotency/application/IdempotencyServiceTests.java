package com.bemo.hr.shared.idempotency.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTests {
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private IdempotencyService service() {
        return new IdempotencyService(idempotencyKeyRepository);
    }

    @Test
    void runsOperationOnceAndStoresTheReference() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(1);
        when(idempotencyKeyRepository.complete(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);

        String result = service().execute("PAYMENT", "op-1", "hash-a",
                () -> "payment-id", value -> value, reference -> reference);

        assertThat(result).isEqualTo("payment-id");
        verify(idempotencyKeyRepository).complete(anyString(), anyString(), anyString(), anyString(), eq("payment-id"));
    }

    @Test
    void replaysTheOriginalResultWhenTheSameRequestIsRepeated() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(completedKey()));

        String first = service().execute("PAYMENT", "op-1", "hash-a",
                () -> {
                    throw new AssertionError("operation must not run on replay");
                },
                value -> value, reference -> "replayed:" + reference);

        assertThat(first).isEqualTo("replayed:payment-id");
    }

    @Test
    void rejectsTheSameKeyUsedWithADifferentRequest() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(completedKey()));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-b",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
    }

    @Test
    void rejectsAKeyThatIsStillInProgress() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.setLeaseExpiresAt(Instant.now().plusSeconds(300));
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already being processed");
        verify(idempotencyKeyRepository, never()).complete(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(idempotencyKeyRepository, never()).fail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void propagatesOperationFailureWithoutStoringASuccessReference() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(1);

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> {
                    throw new IllegalStateException("boom");
                }, value -> value, reference -> reference))
                .isInstanceOf(IllegalStateException.class);
        verify(idempotencyKeyRepository).fail(anyString(), anyString(), anyString(), anyString());
        verify(idempotencyKeyRepository, never()).complete(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void concurrentDuplicateInsertIsResolvedByReloadingAndReplaying() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(completedKey()));

        String result = service().execute("PAYMENT", "op-1", "hash-a",
                () -> {
                    throw new AssertionError("operation must not run after the race");
                },
                value -> value, reference -> "replayed:" + reference);

        assertThat(result).isEqualTo("replayed:payment-id");
    }

    @Test
    void concurrentDuplicateInsertOfADifferentRequestIsRejected() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(completedKey()));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-b",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
    }

    @Test
    void concurrentDuplicateInsertStillInProgressIsRejected() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.setLeaseExpiresAt(Instant.now().plusSeconds(300));
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already being processed");
    }

    @Test
    void anExpiredInProgressReservationIsReclaimedAndTheOperationRuns() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.setLeaseExpiresAt(Instant.now().minusSeconds(30));
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));
        when(idempotencyKeyRepository.steal(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(1);
        when(idempotencyKeyRepository.complete(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(1);

        String result = service().execute("PAYMENT", "op-1", "hash-a",
                () -> "payment-id", value -> value, reference -> reference);

        assertThat(result).isEqualTo("payment-id");
        verify(idempotencyKeyRepository).complete(anyString(), anyString(), anyString(), anyString(), eq("payment-id"));
    }

    @Test
    void aFailedAttemptCanBeRetried() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.fail();
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));
        when(idempotencyKeyRepository.steal(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(1);

        service().execute("PAYMENT", "op-1", "hash-a",
                () -> "payment-id", value -> value, reference -> reference);

        verify(idempotencyKeyRepository).complete(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void aLostStealRaceIsRejected() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.setLeaseExpiresAt(Instant.now().minusSeconds(30));
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));
        when(idempotencyKeyRepository.steal(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(0);

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already being processed");
    }

    @Test
    void aFailedAttemptWithADifferentHashIsRejectedAndNeverReclaimed() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.fail();
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-b",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
        verify(idempotencyKeyRepository, never()).steal(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void anExpiredReservationWithADifferentHashIsRejected() {
        when(idempotencyKeyRepository.reserve(anyString(), anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(0);
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.setLeaseExpiresAt(Instant.now().minusSeconds(30));
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-b",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
        verify(idempotencyKeyRepository, never()).steal(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void renewLeasePushesTheOwnerLeaseOut() {
        service().renewLease("PAYMENT", "op-1", "owner-token");

        verify(idempotencyKeyRepository).renewLease(anyString(), eq("PAYMENT"), eq("op-1"), eq("owner-token"), any());
    }

    @Test
    void renewLeaseWithNoOwnerIsASafeNoOp() {
        service().renewLease("PAYMENT", "op-1", null);

        verify(idempotencyKeyRepository, never()).renewLease(anyString(), anyString(), anyString(), anyString(), any());
    }

    private IdempotencyKey completedKey() {
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.complete("payment-id");
        return key;
    }

    @Test
    void hashIsStableAndNonEmpty() {
        assertThat(IdempotencyService.hash("supplier-a|invoice-1|20.00")).isEqualTo(
                IdempotencyService.hash("supplier-a|invoice-1|20.00"));
        assertThat(IdempotencyService.hash("x")).hasSize(64);
    }
}
