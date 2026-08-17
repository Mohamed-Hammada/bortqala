package com.bemo.hr.operations;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.domain.*;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.numbering.DocumentNumberService;
import com.bemo.hr.shared.security.TenantContext;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryValuationServiceTests {
    @Mock
    private InventoryValuationPolicyRepository policyRepository;
    @Mock
    private InventoryCostLayerRepository layerRepository;
    @Mock
    private InventoryMovementCostRepository movementCostRepository;
    @Mock
    private InventoryRevaluationRepository revaluationRepository;
    @Mock
    private InventoryItemRepository itemRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;
    @Mock
    private FiscalPeriodGuard fiscalPeriodGuard;
    @Mock
    private DocumentNumberService documentNumberService;
    @Mock
    private AuditService auditService;

    private InventoryValuationService service;
    private InventoryItem item;
    private InventoryValuationPolicy policy;
    private FiscalPeriod period;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-valuation");
        service = new InventoryValuationService(policyRepository, layerRepository, movementCostRepository,
                revaluationRepository, itemRepository, movementRepository, accountRepository,
                journalEntryRepository, journalEntryLineRepository, fiscalPeriodGuard,
                documentNumberService, auditService);
        ReflectionTestUtils.setField(service, "companyZone", "Africa/Cairo");
        item = new InventoryItem("RM-1", "Raw material", "RAW_MATERIAL", "KG");
        policy = new InventoryValuationPolicy();
        policy.update(InventoryValuationPolicy.Method.WEIGHTED_AVERAGE, null, null, null, null, false, false);
        period = new FiscalPeriod(2026, 8, "August", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), FiscalPeriod.Status.OPEN);
        lenient().when(policyRepository.findByAppId("app-valuation")).thenReturn(Optional.of(policy));
        lenient().when(itemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        lenient().when(movementCostRepository.findByMovementId(any())).thenReturn(Optional.empty());
        lenient().when(movementCostRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(fiscalPeriodGuard.requireOpen(any())).thenReturn(period);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void weightedAverageValuesReceiptsAndIssuesAndTakesItemLock() {
        StockMovement receipt = movement("10", "PURCHASE_RECEIPT");
        InventoryMovementCost receiptCost = service.valueMovement(receipt, new BigDecimal("5.00"), "admin");
        assertThat(receiptCost.getValueEffect()).isEqualByComparingTo("50.00");

        when(movementRepository.balance(item.getId())).thenReturn(new BigDecimal("6"));
        when(movementCostRepository.inventoryValue(item.getId())).thenReturn(new BigDecimal("50.00"));
        when(revaluationRepository.revaluationValue(item.getId())).thenReturn(BigDecimal.ZERO);
        InventoryMovementCost issueCost = service.valueMovement(movement("-4", "EXPORT_SALE"), null, "admin");

        assertThat(issueCost.getUnitCost()).isEqualByComparingTo("5.000000");
        assertThat(issueCost.getValueEffect()).isEqualByComparingTo("-20.00");
        verify(itemRepository, org.mockito.Mockito.times(2)).findByIdForUpdate(item.getId());
    }

    @Test
    void fifoConsumesOldestLayersAndExplainsTheCost() {
        policy.update(InventoryValuationPolicy.Method.FIFO, null, null, null, null, false, false);
        InventoryCostLayer first = new InventoryCostLayer(item.getId(), "receipt-1", Instant.parse("2026-08-01T08:00:00Z"),
                new BigDecimal("10"), new BigDecimal("5"));
        InventoryCostLayer second = new InventoryCostLayer(item.getId(), "receipt-2", Instant.parse("2026-08-02T08:00:00Z"),
                new BigDecimal("10"), new BigDecimal("7"));
        when(layerRepository.findOpenForUpdate(item.getId())).thenReturn(List.of(first, second));
        when(movementRepository.balance(item.getId())).thenReturn(new BigDecimal("8"));
        InventoryMovementCost cost = service.valueMovement(movement("-12", "EXPORT_SALE"), null, "admin");

        assertThat(cost.getValueEffect()).isEqualByComparingTo("-64.00");
        assertThat(first.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(second.getRemainingQuantity()).isEqualByComparingTo("8");
        assertThat(cost.getExplanation()).contains("10 @ 5", "2 @ 7");
    }

    @Test
    void backdatedMovementIsRejectedBeforeCostMutation() {
        StockMovement movement = movement("3", "PURCHASE_RECEIPT");
        when(movementCostRepository.existsByItemIdAndOccurredAtAfter(item.getId(), movement.getOccurredAt())).thenReturn(true);

        assertThatThrownBy(() -> service.valueMovement(movement, BigDecimal.ONE, "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(error -> assertThat(((BusinessRuleException) error).getCode()).isEqualTo("INV_VAL_BACKDATED_BLOCKED"));
    }

    @Test
    void glPostingCreatesBalancedPostedJournal() {
        policy.update(InventoryValuationPolicy.Method.WEIGHTED_AVERAGE, "inventory", "receipt", "cogs", "adjustment", true, false);
        Account inventory = postingAccount("inventory");
        Account receipt = postingAccount("receipt");
        when(accountRepository.findById("inventory")).thenReturn(Optional.of(inventory));
        when(accountRepository.findById("receipt")).thenReturn(Optional.of(receipt));
        when(documentNumberService.next(any(), any(), any())).thenReturn("INV-2026-00001");
        when(journalEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryMovementCost cost = service.valueMovement(movement("10", "PURCHASE_RECEIPT"), new BigDecimal("5"), "admin");

        assertThat(cost.getJournalEntryId()).isNotBlank();
        ArgumentCaptor<JournalEntryLine> lines = ArgumentCaptor.forClass(JournalEntryLine.class);
        verify(journalEntryLineRepository, org.mockito.Mockito.times(2)).save(lines.capture());
        assertThat(lines.getAllValues().stream().map(JournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("50.00");
        assertThat(lines.getAllValues().stream().map(JournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("50.00");
    }

    @Test
    void returnWithoutExplicitPriceUsesAverageBeforeTheReturn() {
        when(movementRepository.balance(item.getId())).thenReturn(new BigDecimal("12"));
        when(movementCostRepository.inventoryValue(item.getId())).thenReturn(new BigDecimal("50"));
        when(revaluationRepository.revaluationValue(item.getId())).thenReturn(BigDecimal.ZERO);

        InventoryMovementCost returned = service.valueMovement(movement("2", "SALES_RETURN"), null, "admin");

        assertThat(returned.getUnitCost()).isEqualByComparingTo("5.000000");
        assertThat(returned.getValueEffect()).isEqualByComparingTo("10.00");
    }

    @Test
    void closedFiscalPeriodBlocksAValuedMovement() {
        BusinessRuleException closed = new BusinessRuleException("Period is closed", "FISCAL_PERIOD_CLOSED",
                org.springframework.http.HttpStatus.CONFLICT);
        when(fiscalPeriodGuard.requireOpen(any())).thenThrow(closed);

        assertThatThrownBy(() -> service.valueMovement(movement("3", "PURCHASE_RECEIPT"), BigDecimal.ONE, "admin"))
                .isSameAs(closed);
    }

    @Test
    void revaluationReplaysTheSameOperationWithoutPostingTwice() {
        InventoryRevaluation existing = new InventoryRevaluation(item.getId(), "op-1", new BigDecimal("10"),
                new BigDecimal("50"), new BigDecimal("60"), "Market adjustment",
                Instant.parse("2026-08-09T10:00:00Z"), "admin");
        when(revaluationRepository.findByOperationId("op-1")).thenReturn(Optional.of(existing));

        OperationsApi.RevaluationView replay = service.revalue(new OperationsApi.RevaluationRequest(item.getId(),
                new BigDecimal("6"), "Market adjustment", "op-1", Instant.parse("2026-08-09T10:00:00Z")), "admin");

        assertThat(replay.valueDifference()).isEqualByComparingTo("10");
        verify(itemRepository, org.mockito.Mockito.never()).findByIdForUpdate(item.getId());
        verify(journalEntryRepository, org.mockito.Mockito.never()).save(any(JournalEntry.class));
    }

    @Test
    void itemRepositoryUsesPessimisticWriteLockForConcurrentValuation() throws Exception {
        Lock lock = InventoryItemRepository.class.getMethod("findByIdForUpdate", String.class).getAnnotation(Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private StockMovement movement(String quantity, String operation) {
        return new StockMovement(item.getId(), null, operation, new BigDecimal(quantity), null, null, null,
                Instant.parse("2026-08-09T10:00:00Z"), "admin");
    }

    private Account postingAccount(String id) {
        Account account = org.mockito.Mockito.mock(Account.class);
        when(account.isHeader()).thenReturn(false);
        when(account.isActive()).thenReturn(true);
        return account;
    }
}
