package com.bemo.hr.assets.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class AssetsApi {

    private AssetsApi() {
    }

    public record FixedAssetPayload(
            @NotBlank @Size(max = 200) String name,
            @NotBlank String category,
            long acquisitionDate,
            @NotNull @Positive BigDecimal acquisitionCost,
            BigDecimal salvageValue,
            int usefulLifeMonths,
            String branchId,
            String costCenterId
    ) {
    }

    public record DisposalRequest(
            long disposalDate,
            @NotNull BigDecimal proceeds
    ) {
    }

    public record FixedAssetResponse(
            String id, String name, String category, long acquisitionDate,
            BigDecimal acquisitionCost, BigDecimal salvageValue, int usefulLifeMonths,
            BigDecimal monthlyCharge, BigDecimal accumulatedDepreciation,
            BigDecimal netBookValue, String lastPostedYearMonth, String status,
            Long disposalDate, BigDecimal disposalProceeds,
            String branchId, String costCenterId, Long version
    ) {
    }

    public record DepreciationRunResult(
            String assetId, String assetName, BigDecimal charge, String outcome, String entryNumber
    ) {
    }

    public record DepreciationRunResponse(
            String yearMonth, int postedCount, int resultCount, BigDecimal totalCharge,
            List<DepreciationRunResult> results
    ) {
    }

    public record DisposalJournalSummary(
            String journalEntryId, String entryNumber, BigDecimal netBookValue, BigDecimal gainOrLoss
    ) {
    }
}
