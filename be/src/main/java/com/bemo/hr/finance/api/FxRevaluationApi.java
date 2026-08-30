package com.bemo.hr.finance.api;

import java.math.BigDecimal;
import java.util.List;

public class FxRevaluationApi {

    public record RevaluationResponse(
            String id,
            String currencyCode,
            String yearMonth,
            BigDecimal totalUnrealizedGain,
            BigDecimal totalUnrealizedLoss,
            String journalEntryId,
            String postedBy,
            long postedAt,
            long createdAt
    ) {
    }

    public record RevaluationRunResponse(
            int currenciesProcessed,
            int journalsPosted,
            List<CurrencyResult> results
    ) {
    }

    public record CurrencyResult(
            String currencyCode,
            BigDecimal netBalance,
            BigDecimal currentRate,
            BigDecimal bookValueInEgp,
            BigDecimal unrealizedGainLoss,
            String journalEntryId,
            String skippedReason
    ) {
        public CurrencyResult(String currencyCode, BigDecimal netBalance, BigDecimal currentRate,
                              BigDecimal bookValueInEgp, BigDecimal unrealizedGainLoss, String journalEntryId) {
            this(currencyCode, netBalance, currentRate, bookValueInEgp, unrealizedGainLoss, journalEntryId, null);
        }
    }

    public record HistoryResponse(
            List<RevaluationResponse> posts
    ) {
    }
}
