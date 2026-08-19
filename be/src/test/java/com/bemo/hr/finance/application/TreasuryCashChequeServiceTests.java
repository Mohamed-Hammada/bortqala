package com.bemo.hr.finance.application;

import com.bemo.hr.finance.domain.BankAccount;
import com.bemo.hr.finance.domain.treasury.Cashbox;
import com.bemo.hr.finance.domain.treasury.CashboxTransaction;
import com.bemo.hr.finance.domain.treasury.CommercialCheque;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.finance.infrastructure.CashboxTransactionRepository;
import com.bemo.hr.finance.infrastructure.CommercialChequeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TreasuryCashChequeServiceTests {

    @Mock
    private CashboxRepository cashboxRepository;
    @Mock
    private CashboxTransactionRepository cashboxTransactionRepository;
    @Mock
    private CommercialChequeRepository commercialChequeRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private com.bemo.hr.finance.infrastructure.BankStatementRepository bankStatementRepository;

    private TreasuryCashChequeService service;

    @BeforeEach
    void setUp() {
        service = new TreasuryCashChequeService(cashboxRepository, cashboxTransactionRepository,
                commercialChequeRepository, bankAccountRepository, bankStatementRepository);
    }

    @Test
    @DisplayName("Creates cashbox and validates unique code")
    void testCreateCashbox() {
        when(cashboxRepository.existsByCode("MAIN-CASH")).thenReturn(false);
        when(cashboxRepository.save(any(Cashbox.class))).thenAnswer(i -> i.getArgument(0));

        Cashbox cashbox = service.createCashbox("MAIN-CASH", "Main HQ Cashbox", "branch-1", "EGP", "user-1", "acc-1");

        assertThat(cashbox).isNotNull();
        assertThat(cashbox.getCode()).isEqualTo("MAIN-CASH");
        assertThat(cashbox.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cashbox.isActive()).isTrue();

        when(cashboxRepository.existsByCode("MAIN-CASH")).thenReturn(true);
        assertThatThrownBy(() -> service.createCashbox("MAIN-CASH", "Duplicate", null, "EGP", null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("مستخدم بالفعل");
    }

    @Test
    @DisplayName("Records cash receipt and payment adjusting balance correctly")
    void testRecordCashTransactions() {
        Cashbox cashbox = new Cashbox("MAIN-CASH", "Main Cashbox", null, "EGP", null, null);
        when(cashboxRepository.findById(cashbox.getId())).thenReturn(Optional.of(cashbox));
        when(cashboxTransactionRepository.save(any(CashboxTransaction.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Receipt of 10,000 EGP
        CashboxTransaction receipt = service.recordCashTransaction(
                cashbox.getId(), CashboxTransaction.TransactionType.RECEIPT, BigDecimal.valueOf(10000),
                "RV-001", "cust-1", "Customer down payment", System.currentTimeMillis(), "cashier"
        );
        assertThat(receipt).isNotNull();
        assertThat(cashbox.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(10000));

        // 2. Payment of 3,000 EGP
        CashboxTransaction payment = service.recordCashTransaction(
                cashbox.getId(), CashboxTransaction.TransactionType.PAYMENT, BigDecimal.valueOf(3000),
                "PV-001", "supp-1", "Site supplies payment", System.currentTimeMillis(), "cashier"
        );
        assertThat(payment).isNotNull();
        assertThat(cashbox.getCurrentBalance()).isEqualByComparingTo(BigDecimal.valueOf(7000));

        // 3. Payment exceeding current balance throws exception
        assertThatThrownBy(() -> service.recordCashTransaction(
                cashbox.getId(), CashboxTransaction.TransactionType.PAYMENT, BigDecimal.valueOf(15000),
                "PV-002", null, "Overdraft", System.currentTimeMillis(), "cashier"
        )).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("رصيد الخزينة الحالي لا يكفي");
    }

    @Test
    @DisplayName("Cheque lifecycle: received -> deposited -> collected / bounced / cancelled")
    void testChequeLifecycle() {
        when(commercialChequeRepository.save(any(CommercialCheque.class))).thenAnswer(i -> i.getArgument(0));

        // 1. Register received cheque
        CommercialCheque cheque = service.registerCheque(
                "CHQ-987654", CommercialCheque.ChequeType.RECEIVED, "CIB", null,
                "Al-Ahram Trading", "party-10", BigDecimal.valueOf(50000), "EGP",
                System.currentTimeMillis(), System.currentTimeMillis() + 864000000L, "Advance payment"
        );
        assertThat(cheque.getStatus()).isEqualTo(CommercialCheque.Status.RECEIVED);

        when(commercialChequeRepository.findById(cheque.getId())).thenReturn(Optional.of(cheque));
        when(bankAccountRepository.findById("bank-acc-1")).thenReturn(Optional.of(mock(BankAccount.class)));

        // 2. Deposit to bank
        service.depositCheque(cheque.getId(), "bank-acc-1");
        assertThat(cheque.getStatus()).isEqualTo(CommercialCheque.Status.DEPOSITED);
        assertThat(cheque.getBankAccountId()).isEqualTo("bank-acc-1");

        // 3. Bounce with reason
        service.bounceCheque(cheque.getId(), "Signature mismatch");
        assertThat(cheque.getStatus()).isEqualTo(CommercialCheque.Status.BOUNCED);
        assertThat(cheque.getBounceReason()).isEqualTo("Signature mismatch");

        // 4. Re-deposit and collect
        service.depositCheque(cheque.getId(), "bank-acc-1");
        service.collectCheque(cheque.getId());
        assertThat(cheque.getStatus()).isEqualTo(CommercialCheque.Status.COLLECTED);
    }

    @Test
    @DisplayName("Computes unified liquidity summary aggregating banks, cashboxes and cheques")
    void testUnifiedLiquiditySummary() {
        BankAccount b1 = mock(BankAccount.class);
        when(b1.getId()).thenReturn("b1");
        when(b1.isActive()).thenReturn(true);
        when(bankAccountRepository.findAll()).thenReturn(List.of(b1));

        com.bemo.hr.finance.domain.BankStatement stmt = mock(com.bemo.hr.finance.domain.BankStatement.class);
        when(stmt.getClosingBalance()).thenReturn(BigDecimal.valueOf(150000));
        when(bankStatementRepository.findFirstByBankAccountIdOrderByPeriodEndDesc("b1")).thenReturn(Optional.of(stmt));

        Cashbox c1 = new Cashbox("C1", "Cashbox 1", null, "EGP", null, null);
        c1.adjustBalance(BigDecimal.valueOf(25000));
        when(cashboxRepository.findAll()).thenReturn(List.of(c1));

        CommercialCheque chqRec = new CommercialCheque("CHQ-1", CommercialCheque.ChequeType.RECEIVED, "CIB", null, "Client", null, BigDecimal.valueOf(40000), "EGP", 0, 0, null);
        CommercialCheque chqIss = new CommercialCheque("CHQ-2", CommercialCheque.ChequeType.ISSUED, "NBE", null, "Supplier", null, BigDecimal.valueOf(15000), "EGP", 0, 0, null);
        when(commercialChequeRepository.findAll()).thenReturn(List.of(chqRec, chqIss));

        TreasuryCashChequeService.UnifiedLiquiditySummary summary = service.getUnifiedLiquiditySummary();

        assertThat(summary.totalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(150000));
        assertThat(summary.totalCashBalance()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(summary.chequesUnderCollection()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(summary.chequesIssuedOutstanding()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        // Net: 150000 + 25000 + 40000 - 15000 = 200000
        assertThat(summary.netLiquidityPosition()).isEqualByComparingTo(BigDecimal.valueOf(200000));
    }
}
