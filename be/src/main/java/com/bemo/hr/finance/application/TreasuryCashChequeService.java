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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional
public class TreasuryCashChequeService {

    private final CashboxRepository cashboxRepository;
    private final CashboxTransactionRepository cashboxTransactionRepository;
    private final CommercialChequeRepository commercialChequeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final com.bemo.hr.finance.infrastructure.BankStatementRepository bankStatementRepository;

    public TreasuryCashChequeService(CashboxRepository cashboxRepository,
                                     CashboxTransactionRepository cashboxTransactionRepository,
                                     CommercialChequeRepository commercialChequeRepository,
                                     BankAccountRepository bankAccountRepository,
                                     com.bemo.hr.finance.infrastructure.BankStatementRepository bankStatementRepository) {
        this.cashboxRepository = cashboxRepository;
        this.cashboxTransactionRepository = cashboxTransactionRepository;
        this.commercialChequeRepository = commercialChequeRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.bankStatementRepository = bankStatementRepository;
    }

    @Transactional(readOnly = true)
    public List<Cashbox> listCashboxes() {
        return cashboxRepository.findAllByOrderByCreatedAtDesc();
    }

    public Cashbox createCashbox(String code, String name, String branchId, String currency, String custodianUserId, String glAccountId) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("كود الخزينة مطلوب", "CASHBOX_CODE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("اسم الخزينة مطلوب", "CASHBOX_NAME_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (cashboxRepository.existsByCode(code.strip())) {
            throw new BusinessRuleException("كود الخزينة مستخدم بالفعل", "CASHBOX_CODE_DUPLICATE", HttpStatus.CONFLICT);
        }
        Cashbox cashbox = new Cashbox(code, name, branchId, currency, custodianUserId, glAccountId);
        return cashboxRepository.save(cashbox);
    }

    public Cashbox updateCashbox(String id, String name, String branchId, String custodianUserId, String glAccountId, boolean active) {
        Cashbox cashbox = cashboxRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("الخزينة غير موجودة", "CASHBOX_NOT_FOUND", HttpStatus.NOT_FOUND));
        cashbox.updateDetails(name, branchId, custodianUserId, glAccountId, active);
        return cashboxRepository.save(cashbox);
    }

    public CashboxTransaction recordCashTransaction(String cashboxId, CashboxTransaction.TransactionType type,
                                                   BigDecimal amount, String voucherNumber, String counterpartyPartyId,
                                                   String description, long dateMs, String username) {
        Cashbox cashbox = cashboxRepository.findById(cashboxId)
                .orElseThrow(() -> new BusinessRuleException("الخزينة غير موجودة", "CASHBOX_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!cashbox.isActive()) {
            throw new BusinessRuleException("لا يمكن إجراء حركات على خزينة غير مفعلة", "CASHBOX_INACTIVE", HttpStatus.CONFLICT);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("مبلغ الحركة النقدية يجب أن يكون أكبر من الصفر", "INVALID_CASH_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        BigDecimal delta;
        switch (type) {
            case RECEIPT, PETTY_CASH_SETTLEMENT -> delta = amount;
            case PAYMENT, PETTY_CASH_ADVANCE -> {
                if (cashbox.getCurrentBalance().compareTo(amount) < 0) {
                    throw new BusinessRuleException("رصيد الخزينة الحالي لا يكفي لإتمام عملية الصرف", "INSUFFICIENT_CASHBOX_BALANCE", HttpStatus.CONFLICT);
                }
                delta = amount.negate();
            }
            case PHYSICAL_COUNT_ADJUSTMENT -> {
                // In physical count adjustment, amount represents target actual counted cash
                delta = amount.subtract(cashbox.getCurrentBalance());
            }
            default -> delta = BigDecimal.ZERO;
        }

        cashbox.adjustBalance(delta);
        cashboxRepository.save(cashbox);

        CashboxTransaction tx = new CashboxTransaction(cashboxId, type, amount, voucherNumber, counterpartyPartyId,
                description, dateMs, username);
        return cashboxTransactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<CashboxTransaction> listCashboxTransactions(String cashboxId) {
        return cashboxTransactionRepository.findByCashboxIdOrderByTransactionDateDescCreatedAtDesc(cashboxId);
    }

    @Transactional(readOnly = true)
    public List<CommercialCheque> listCheques(CommercialCheque.ChequeType type, CommercialCheque.Status status) {
        if (type != null) {
            return commercialChequeRepository.findByChequeTypeOrderByDueDateAsc(type);
        }
        if (status != null) {
            return commercialChequeRepository.findByStatusOrderByDueDateAsc(status);
        }
        return commercialChequeRepository.findAllByOrderByDueDateAscCreatedAtDesc();
    }

    public CommercialCheque registerCheque(String chequeNumber, CommercialCheque.ChequeType chequeType,
                                           String bankName, String bankAccountId, String drawerPayeeName,
                                           String partyId, BigDecimal amount, String currency,
                                           long issueDate, long dueDate, String notes) {
        if (chequeNumber == null || chequeNumber.isBlank()) {
            throw new BusinessRuleException("رقم الشيك مطلوب", "CHEQUE_NUMBER_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (drawerPayeeName == null || drawerPayeeName.isBlank()) {
            throw new BusinessRuleException("اسم الساحب / المستفيد مطلوب", "DRAWER_PAYEE_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("مبلغ الشيك يجب أن يكون أكبر من الصفر", "INVALID_CHEQUE_AMOUNT", HttpStatus.BAD_REQUEST);
        }

        CommercialCheque cheque = new CommercialCheque(chequeNumber, chequeType, bankName, bankAccountId,
                drawerPayeeName, partyId, amount, currency, issueDate, dueDate, notes);
        return commercialChequeRepository.save(cheque);
    }

    public CommercialCheque depositCheque(String chequeId, String targetBankAccountId) {
        CommercialCheque cheque = commercialChequeRepository.findById(chequeId)
                .orElseThrow(() -> new BusinessRuleException("الشيك غير موجود", "CHEQUE_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (targetBankAccountId != null && !targetBankAccountId.isBlank()) {
            bankAccountRepository.findById(targetBankAccountId)
                    .orElseThrow(() -> new BusinessRuleException("الحساب البنكي غير موجود", "BANK_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));
        }
        cheque.deposit(targetBankAccountId);
        return commercialChequeRepository.save(cheque);
    }

    public CommercialCheque collectCheque(String chequeId) {
        CommercialCheque cheque = commercialChequeRepository.findById(chequeId)
                .orElseThrow(() -> new BusinessRuleException("الشيك غير موجود", "CHEQUE_NOT_FOUND", HttpStatus.NOT_FOUND));
        cheque.collect();
        return commercialChequeRepository.save(cheque);
    }

    public CommercialCheque bounceCheque(String chequeId, String reason) {
        CommercialCheque cheque = commercialChequeRepository.findById(chequeId)
                .orElseThrow(() -> new BusinessRuleException("الشيك غير موجود", "CHEQUE_NOT_FOUND", HttpStatus.NOT_FOUND));
        cheque.bounce(reason);
        return commercialChequeRepository.save(cheque);
    }

    public CommercialCheque cancelCheque(String chequeId, String reason) {
        CommercialCheque cheque = commercialChequeRepository.findById(chequeId)
                .orElseThrow(() -> new BusinessRuleException("الشيك غير موجود", "CHEQUE_NOT_FOUND", HttpStatus.NOT_FOUND));
        cheque.cancel(reason);
        return commercialChequeRepository.save(cheque);
    }

    @Transactional(readOnly = true)
    public UnifiedLiquiditySummary getUnifiedLiquiditySummary() {
        BigDecimal totalBank = bankAccountRepository.findAll().stream()
                .filter(BankAccount::isActive)
                .map(bank -> bankStatementRepository.findFirstByBankAccountIdOrderByPeriodEndDesc(bank.getId())
                        .map(com.bemo.hr.finance.domain.BankStatement::getClosingBalance)
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCash = cashboxRepository.findAll().stream()
                .filter(Cashbox::isActive)
                .map(Cashbox::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CommercialCheque> allCheques = commercialChequeRepository.findAll();

        BigDecimal chequesReceivedUnderCollection = allCheques.stream()
                .filter(c -> c.getChequeType() == CommercialCheque.ChequeType.RECEIVED)
                .filter(c -> c.getStatus() == CommercialCheque.Status.RECEIVED || c.getStatus() == CommercialCheque.Status.DEPOSITED)
                .map(CommercialCheque::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal chequesIssuedOutstanding = allCheques.stream()
                .filter(c -> c.getChequeType() == CommercialCheque.ChequeType.ISSUED)
                .filter(c -> c.getStatus() == CommercialCheque.Status.ISSUED)
                .map(CommercialCheque::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netLiquidity = totalBank.add(totalCash).add(chequesReceivedUnderCollection).subtract(chequesIssuedOutstanding);

        return new UnifiedLiquiditySummary(
                totalBank,
                totalCash,
                chequesReceivedUnderCollection,
                chequesIssuedOutstanding,
                netLiquidity
        );
    }

    public record UnifiedLiquiditySummary(
            BigDecimal totalBankBalance,
            BigDecimal totalCashBalance,
            BigDecimal chequesUnderCollection,
            BigDecimal chequesIssuedOutstanding,
            BigDecimal netLiquidityPosition
    ) {}
}
