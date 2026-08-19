package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.TreasuryCashChequeService;
import com.bemo.hr.finance.domain.treasury.Cashbox;
import com.bemo.hr.finance.domain.treasury.CashboxTransaction;
import com.bemo.hr.finance.domain.treasury.CommercialCheque;
import com.bemo.hr.shared.security.Roles;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance/treasury")
@PreAuthorize(Roles.ADMIN_ONLY + " or " + Roles.ACCOUNTANT + " or " + Roles.FINANCE_MANAGER + " or " + Roles.TREASURY_MANAGER)
public class TreasuryCashChequeController {

    private final TreasuryCashChequeService service;

    public TreasuryCashChequeController(TreasuryCashChequeService service) {
        this.service = service;
    }

    @GetMapping("/cashboxes")
    public List<Cashbox> listCashboxes() {
        return service.listCashboxes();
    }

    @PostMapping("/cashboxes")
    @ResponseStatus(HttpStatus.CREATED)
    public Cashbox createCashbox(@Valid @RequestBody CreateCashboxRequest request) {
        return service.createCashbox(request.code(), request.name(), request.branchId(),
                request.currency(), request.custodianUserId(), request.glAccountId());
    }

    @PutMapping("/cashboxes/{id}")
    public Cashbox updateCashbox(@PathVariable String id, @Valid @RequestBody UpdateCashboxRequest request) {
        return service.updateCashbox(id, request.name(), request.branchId(), request.custodianUserId(),
                request.glAccountId(), request.active());
    }

    @GetMapping("/cashboxes/{id}/transactions")
    public List<CashboxTransaction> listCashboxTransactions(@PathVariable String id) {
        return service.listCashboxTransactions(id);
    }

    @PostMapping("/cashboxes/{id}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public CashboxTransaction recordCashTransaction(@PathVariable String id,
                                                    @Valid @RequestBody RecordCashTransactionRequest request,
                                                    @AuthenticationPrincipal String username) {
        return service.recordCashTransaction(id, request.transactionType(), request.amount(),
                request.voucherNumber(), request.counterpartyPartyId(), request.description(),
                request.transactionDate(), username != null ? username : "SYSTEM");
    }

    @GetMapping("/cheques")
    public List<CommercialCheque> listCheques(@RequestParam(required = false) CommercialCheque.ChequeType type,
                                              @RequestParam(required = false) CommercialCheque.Status status) {
        return service.listCheques(type, status);
    }

    @PostMapping("/cheques")
    @ResponseStatus(HttpStatus.CREATED)
    public CommercialCheque registerCheque(@Valid @RequestBody RegisterChequeRequest request) {
        return service.registerCheque(request.chequeNumber(), request.chequeType(), request.bankName(),
                request.bankAccountId(), request.drawerPayeeName(), request.partyId(), request.amount(),
                request.currency(), request.issueDate(), request.dueDate(), request.notes());
    }

    @PostMapping("/cheques/{id}/deposit")
    public CommercialCheque depositCheque(@PathVariable String id, @RequestBody(required = false) DepositChequeRequest request) {
        return service.depositCheque(id, request != null ? request.targetBankAccountId() : null);
    }

    @PostMapping("/cheques/{id}/collect")
    public CommercialCheque collectCheque(@PathVariable String id) {
        return service.collectCheque(id);
    }

    @PostMapping("/cheques/{id}/bounce")
    public CommercialCheque bounceCheque(@PathVariable String id, @RequestBody(required = false) ChequeActionReasonRequest request) {
        return service.bounceCheque(id, request != null ? request.reason() : null);
    }

    @PostMapping("/cheques/{id}/cancel")
    public CommercialCheque cancelCheque(@PathVariable String id, @RequestBody(required = false) ChequeActionReasonRequest request) {
        return service.cancelCheque(id, request != null ? request.reason() : null);
    }

    @GetMapping("/liquidity-summary")
    public TreasuryCashChequeService.UnifiedLiquiditySummary getLiquiditySummary() {
        return service.getUnifiedLiquiditySummary();
    }

    public record CreateCashboxRequest(
            @NotBlank String code,
            @NotBlank String name,
            String branchId,
            String currency,
            String custodianUserId,
            String glAccountId
    ) {}

    public record UpdateCashboxRequest(
            @NotBlank String name,
            String branchId,
            String custodianUserId,
            String glAccountId,
            boolean active
    ) {}

    public record RecordCashTransactionRequest(
            @NotNull CashboxTransaction.TransactionType transactionType,
            @NotNull BigDecimal amount,
            String voucherNumber,
            String counterpartyPartyId,
            String description,
            long transactionDate
    ) {}

    public record RegisterChequeRequest(
            @NotBlank String chequeNumber,
            @NotNull CommercialCheque.ChequeType chequeType,
            String bankName,
            String bankAccountId,
            @NotBlank String drawerPayeeName,
            String partyId,
            @NotNull BigDecimal amount,
            String currency,
            long issueDate,
            long dueDate,
            String notes
    ) {}

    public record DepositChequeRequest(String targetBankAccountId) {}
    public record ChequeActionReasonRequest(String reason) {}
}
