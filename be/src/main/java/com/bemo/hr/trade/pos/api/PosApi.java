package com.bemo.hr.trade.pos.api;

import com.bemo.hr.trade.pos.domain.*;

import java.math.BigDecimal;
import java.util.List;

public final class PosApi {

    private PosApi() {
    }

    public record SaveTerminalRequest(
            String terminalCode,
            String terminalName,
            String branchId,
            String warehouseId,
            String cashboxId,
            PosTerminalStatus status
    ) {}

    public record TerminalResponse(
            String id,
            String terminalCode,
            String terminalName,
            String branchId,
            String warehouseId,
            String cashboxId,
            PosTerminalStatus status,
            long createdAt,
            long updatedAt
    ) {}

    public record OpenSessionRequest(
            String terminalId,
            BigDecimal openingFloat
    ) {}

    public record CloseSessionRequest(
            BigDecimal closingActualCash,
            BigDecimal closingActualCard,
            String notes
    ) {}

    public record SessionResponse(
            String id,
            String sessionNumber,
            String terminalId,
            String cashierUserId,
            long openedAt,
            Long closedAt,
            BigDecimal openingFloat,
            BigDecimal closingActualCash,
            BigDecimal closingCalculatedCash,
            BigDecimal closingActualCard,
            BigDecimal closingCalculatedCard,
            BigDecimal cashVariance,
            BigDecimal cardVariance,
            PosSessionStatus status,
            String notes,
            long createdAt,
            long updatedAt
    ) {}

    public record PosLineItem(
            String itemId,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal lineTotal,
            String notes
    ) {}

    public record ProcessSaleRequest(
            String sessionId,
            String customerId,
            PosPaymentMethod paymentMethod,
            BigDecimal cashTendered,
            String clientOfflineId,
            List<PosLineItem> lines
    ) {}

    public record ProcessReturnRequest(
            String originalTransactionId,
            String sessionId,
            String reason,
            List<PosLineItem> returnLines
    ) {}

    public record TransactionResponse(
            String id,
            String transactionNumber,
            String sessionId,
            String terminalId,
            String cashierUserId,
            String customerId,
            PosTransactionType transactionType,
            PosPaymentMethod paymentMethod,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            BigDecimal cashTendered,
            BigDecimal changeAmount,
            PosTransactionStatus status,
            String originalTransactionId,
            String clientOfflineId,
            long createdAt,
            List<PosLineItem> lines
    ) {}

    public record PosSummaryResponse(
            BigDecimal todaySales,
            long todayTransactionsCount,
            long activeShiftsCount,
            BigDecimal totalVariance
    ) {}
}
