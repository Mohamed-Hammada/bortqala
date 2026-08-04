package com.bemo.hr.shared.idempotency.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.idempotency.domain.IdempotencyKey;
import com.bemo.hr.shared.idempotency.infrastructure.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTests {
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private IdempotencyService service() {
        return new IdempotencyService(idempotencyKeyRepository);
    }

    @Test
    void runsOperationOnceAndStoresTheReference() {
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1")).thenReturn(Optional.empty());

        String result = service().execute("PAYMENT", "op-1", "hash-a",
                () -> "payment-id", value -> value, reference -> reference);

        assertThat(result).isEqualTo("payment-id");
        verify(idempotencyKeyRepository, atLeastOnce()).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    void replaysTheOriginalResultWhenTheSameRequestIsRepeated() {
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.complete("payment-id");
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        String first = service().execute("PAYMENT", "op-1", "hash-a",
                () -> { throw new AssertionError("operation must not run on replay"); },
                value -> value, reference -> "replayed:" + reference);

        assertThat(first).isEqualTo("replayed:payment-id");
    }

    @Test
    void rejectsTheSameKeyUsedWithADifferentRequest() {
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        key.complete("payment-id");
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-b",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
    }

    @Test
    void rejectsAKeyThatIsStillInProgress() {
        var key = new IdempotencyKey("PAYMENT", "op-1", "hash-a");
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class);
        verify(idempotencyKeyRepository, never()).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    void propagatesOperationFailureWithoutStoringASuccessReference() {
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> { throw new IllegalStateException("boom"); }, value -> value, reference -> reference))
                .isInstanceOf(IllegalStateException.class);
        verify(idempotencyKeyRepository, atLeastOnce()).saveAndFlush(any(IdempotencyKey.class));
    }

    @Test
    void concurrentDuplicateInsertIsResolvedByReloadingAndReplaying() {
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(completedKey()));
        when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        String result = service().execute("PAYMENT", "op-1", "hash-a",
                () -> { throw new AssertionError("operation must not run after the race"); },
                value -> value, reference -> "replayed:" + reference);

        assertThat(result).isEqualTo("replayed:payment-id");
    }

    @Test
    void concurrentDuplicateInsertOfADifferentRequestIsRejected() {
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(completedKey()));
        when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-b",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different request");
    }

    @Test
    void concurrentDuplicateInsertStillInProgressIsRejected() {
        when(idempotencyKeyRepository.findByOperationTypeAndOperationId("PAYMENT", "op-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IdempotencyKey("PAYMENT", "op-1", "hash-a")));
        when(idempotencyKeyRepository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service().execute("PAYMENT", "op-1", "hash-a",
                () -> "ignored", value -> value, reference -> reference))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already being processed");
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
