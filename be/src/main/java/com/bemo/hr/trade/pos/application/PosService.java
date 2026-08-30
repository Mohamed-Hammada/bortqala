package com.bemo.hr.trade.pos.application;

import com.bemo.hr.shared.domain.NotFoundException;
import com.bemo.hr.trade.pos.api.PosApi;
import com.bemo.hr.trade.pos.domain.*;
import com.bemo.hr.trade.pos.infrastructure.PosSessionRepository;
import com.bemo.hr.trade.pos.infrastructure.PosTerminalRepository;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionLineRepository;
import com.bemo.hr.trade.pos.infrastructure.PosTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PosService {

    private final PosTerminalRepository terminalRepository;
    private final PosSessionRepository sessionRepository;
    private final PosTransactionRepository transactionRepository;
    private final PosTransactionLineRepository transactionLineRepository;

    public PosService(PosTerminalRepository terminalRepository,
                      PosSessionRepository sessionRepository,
                      PosTransactionRepository transactionRepository,
                      PosTransactionLineRepository transactionLineRepository) {
        this.terminalRepository = terminalRepository;
        this.sessionRepository = sessionRepository;
        this.transactionRepository = transactionRepository;
        this.transactionLineRepository = transactionLineRepository;
    }

    public PosApi.TerminalResponse saveTerminal(PosApi.SaveTerminalRequest request) {
        Optional<PosTerminal> existing = terminalRepository.findByTerminalCode(request.terminalCode());
        PosTerminal terminal;
        if (existing.isPresent()) {
            terminal = existing.get();
            terminal.update(
                    request.terminalName(),
                    request.branchId(),
                    request.warehouseId(),
                    request.cashboxId(),
                    request.status() != null ? request.status() : PosTerminalStatus.ACTIVE
            );
        } else {
            terminal = new PosTerminal(
                    request.terminalCode(),
                    request.terminalName(),
                    request.branchId(),
                    request.warehouseId(),
                    request.cashboxId()
            );
        }
        PosTerminal saved = terminalRepository.save(terminal);
        return toTerminalResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PosApi.TerminalResponse> listTerminals() {
        return terminalRepository.findAllByOrderByTerminalCodeAsc().stream()
                .map(this::toTerminalResponse)
                .toList();
    }

    public PosApi.SessionResponse openSession(String cashierUserId, PosApi.OpenSessionRequest request) {
        PosTerminal terminal = terminalRepository.findById(request.terminalId())
                .orElseThrow(() -> new NotFoundException("POS Terminal not found: " + request.terminalId()));

        Optional<PosSession> active = sessionRepository.findFirstByTerminalIdAndStatus(terminal.getId(), PosSessionStatus.OPEN);
        if (active.isPresent()) {
            return toSessionResponse(active.get());
        }

        int year = LocalDate.now().getYear();
        long count = sessionRepository.count() + 1;
        String sessionNumber = String.format("POS-SES-%d-%03d", year, count);

        PosSession session = new PosSession(sessionNumber, terminal.getId(), cashierUserId, request.openingFloat());
        PosSession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PosApi.SessionResponse> getActiveSession(String terminalId) {
        return sessionRepository.findFirstByTerminalIdAndStatus(terminalId, PosSessionStatus.OPEN)
                .map(this::toSessionResponse);
    }

    @Transactional(readOnly = true)
    public List<PosApi.SessionResponse> listSessions() {
        return sessionRepository.findAllByOrderByOpenedAtDesc().stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public PosApi.SessionResponse closeSession(String sessionId, PosApi.CloseSessionRequest request) {
        PosSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("POS Session not found: " + sessionId));

        session.close(request.closingActualCash(), request.closingActualCard(), request.notes());
        PosSession saved = sessionRepository.save(session);
        return toSessionResponse(saved);
    }

    public PosApi.TransactionResponse processSale(String cashierUserId, PosApi.ProcessSaleRequest request) {
        if (request.clientOfflineId() != null && !request.clientOfflineId().isBlank()) {
            Optional<PosTransaction> existing = transactionRepository.findByClientOfflineId(request.clientOfflineId());
            if (existing.isPresent()) {
                return toTransactionResponse(existing.get());
            }
        }

        PosSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Active POS Session required: " + request.sessionId()));

        if (session.getStatus() != PosSessionStatus.OPEN) {
            throw new IllegalStateException("Cannot process transaction on closed shift");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<PosTransactionLine> domainLines = new ArrayList<>();
        int year = LocalDate.now().getYear();
        long count = transactionRepository.count() + 1;
        String txnNumber = String.format("POS-TXN-%d-%04d", year, count);

        for (PosApi.PosLineItem line : request.lines()) {
            BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ONE;
            BigDecimal price = line.unitPrice() != null ? line.unitPrice() : BigDecimal.ZERO;
            BigDecimal lineSub = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discRate = line.discountRate() != null ? line.discountRate() : BigDecimal.ZERO;
            BigDecimal lineDisc = lineSub.multiply(discRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal net = lineSub.subtract(lineDisc);
            BigDecimal lineTax = net.multiply(new BigDecimal("0.14")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = net.add(lineTax);

            subtotal = subtotal.add(lineSub);
            totalDiscount = totalDiscount.add(lineDisc);
            totalTax = totalTax.add(lineTax);
            totalAmount = totalAmount.add(lineTotal);

            domainLines.add(new PosTransactionLine(
                    null,
                    line.itemId(),
                    line.itemCode(),
                    line.itemName(),
                    qty,
                    price,
                    discRate,
                    lineDisc,
                    lineTax,
                    lineTotal,
                    line.notes()
            ));
        }

        BigDecimal cashTendered = request.cashTendered() != null ? request.cashTendered() : totalAmount;
        BigDecimal changeAmount = BigDecimal.ZERO;

        if (request.paymentMethod() == PosPaymentMethod.CASH) {
            if (cashTendered.compareTo(totalAmount) >= 0) {
                changeAmount = cashTendered.subtract(totalAmount);
            }
            session.addSaleTotals(totalAmount, BigDecimal.ZERO);
        } else if (request.paymentMethod() == PosPaymentMethod.CARD || request.paymentMethod() == PosPaymentMethod.WALLET) {
            session.addSaleTotals(BigDecimal.ZERO, totalAmount);
        } else if (request.paymentMethod() == PosPaymentMethod.SPLIT) {
            BigDecimal cashPart = cashTendered.min(totalAmount);
            BigDecimal cardPart = totalAmount.subtract(cashPart);
            session.addSaleTotals(cashPart, cardPart);
        } else {
            session.addSaleTotals(BigDecimal.ZERO, totalAmount);
        }

        PosTransaction transaction = new PosTransaction(
                txnNumber,
                session.getId(),
                session.getTerminalId(),
                cashierUserId,
                request.customerId(),
                PosTransactionType.SALE,
                request.paymentMethod(),
                subtotal,
                totalDiscount,
                totalTax,
                totalAmount,
                cashTendered,
                changeAmount,
                null,
                request.clientOfflineId()
        );

        PosTransaction savedTxn = transactionRepository.save(transaction);

        for (PosTransactionLine line : domainLines) {
            PosTransactionLine pers = new PosTransactionLine(
                    savedTxn.getId(),
                    line.getItemId(),
                    line.getItemCode(),
                    line.getItemName(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getDiscountRate(),
                    line.getDiscountAmount(),
                    line.getTaxAmount(),
                    line.getLineTotal(),
                    line.getNotes()
            );
            transactionLineRepository.save(pers);
        }

        sessionRepository.save(session);
        return toTransactionResponse(savedTxn);
    }

    public PosApi.TransactionResponse processReturn(String cashierUserId, PosApi.ProcessReturnRequest request) {
        PosTransaction original = transactionRepository.findById(request.originalTransactionId())
                .orElseThrow(() -> new NotFoundException("Original Transaction not found: " + request.originalTransactionId()));

        PosSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Active POS Session required: " + request.sessionId()));

        int year = LocalDate.now().getYear();
        long count = transactionRepository.count() + 1;
        String returnNumber = String.format("POS-RET-%d-%04d", year, count);

        BigDecimal totalRefund = original.getTotalAmount().negate();
        if (original.getPaymentMethod() == PosPaymentMethod.CASH) {
            session.addReturnTotals(original.getTotalAmount(), BigDecimal.ZERO);
        } else {
            session.addReturnTotals(BigDecimal.ZERO, original.getTotalAmount());
        }

        PosTransaction returnTxn = new PosTransaction(
                returnNumber,
                session.getId(),
                session.getTerminalId(),
                cashierUserId,
                original.getCustomerId(),
                PosTransactionType.RETURN,
                original.getPaymentMethod(),
                original.getSubtotal().negate(),
                original.getDiscountAmount().negate(),
                original.getTaxAmount().negate(),
                totalRefund,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                original.getId(),
                null
        );

        PosTransaction savedReturn = transactionRepository.save(returnTxn);
        original.markRefunded();
        transactionRepository.save(original);
        sessionRepository.save(session);

        return toTransactionResponse(savedReturn);
    }

    @Transactional(readOnly = true)
    public List<PosApi.TransactionResponse> listTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosApi.PosSummaryResponse getSummary() {
        long startOfDay = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        BigDecimal todaySales = transactionRepository.sumTodaySales(startOfDay);
        long todayTransactions = transactionRepository.countTodayTransactions(startOfDay);
        long activeShifts = sessionRepository.countByStatus(PosSessionStatus.OPEN);

        return new PosApi.PosSummaryResponse(
                todaySales != null ? todaySales : BigDecimal.ZERO,
                todayTransactions,
                activeShifts,
                BigDecimal.ZERO
        );
    }

    private PosApi.TerminalResponse toTerminalResponse(PosTerminal t) {
        return new PosApi.TerminalResponse(
                t.getId(),
                t.getTerminalCode(),
                t.getTerminalName(),
                t.getBranchId(),
                t.getWarehouseId(),
                t.getCashboxId(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private PosApi.SessionResponse toSessionResponse(PosSession s) {
        return new PosApi.SessionResponse(
                s.getId(),
                s.getSessionNumber(),
                s.getTerminalId(),
                s.getCashierUserId(),
                s.getOpenedAt(),
                s.getClosedAt(),
                s.getOpeningFloat(),
                s.getClosingActualCash(),
                s.getClosingCalculatedCash(),
                s.getClosingActualCard(),
                s.getClosingCalculatedCard(),
                s.getCashVariance(),
                s.getCardVariance(),
                s.getStatus(),
                s.getNotes(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private PosApi.TransactionResponse toTransactionResponse(PosTransaction t) {
        List<PosTransactionLine> lines = transactionLineRepository.findAllByTransactionId(t.getId());
        List<PosApi.PosLineItem> lineItems = lines.stream()
                .map(l -> new PosApi.PosLineItem(
                        l.getItemId(),
                        l.getItemCode(),
                        l.getItemName(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getDiscountRate(),
                        l.getDiscountAmount(),
                        l.getTaxAmount(),
                        l.getLineTotal(),
                        l.getNotes()
                ))
                .toList();

        return new PosApi.TransactionResponse(
                t.getId(),
                t.getTransactionNumber(),
                t.getSessionId(),
                t.getTerminalId(),
                t.getCashierUserId(),
                t.getCustomerId(),
                t.getTransactionType(),
                t.getPaymentMethod(),
                t.getSubtotal(),
                t.getDiscountAmount(),
                t.getTaxAmount(),
                t.getTotalAmount(),
                t.getCashTendered(),
                t.getChangeAmount(),
                t.getStatus(),
                t.getOriginalTransactionId(),
                t.getClientOfflineId(),
                t.getCreatedAt(),
                lineItems
        );
    }
}
