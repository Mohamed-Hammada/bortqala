package com.bemo.hr.growth.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class GrowthPackApi {

    // ─── Loyalty ──────────────────────────────────────────────

    public record LoyaltyAccountResponse(
            String id, String partyId, BigDecimal pointsBalance,
            BigDecimal totalEarned, BigDecimal totalRedeemed, BigDecimal totalExpired,
            long createdAt
    ) {}

    public record LoyaltyLedgerEntryResponse(
            String id, String loyaltyAccountId, String partyId, String type,
            BigDecimal points, BigDecimal runningBalance,
            String referenceType, String referenceId, String ruleSnapshot,
            String notes, String actor, long createdAt
    ) {}

    public record EarnPointsPayload(
            @NotBlank String partyId,
            @NotNull @DecimalMin("0.01") BigDecimal points,
            String referenceType, String referenceId, String ruleSnapshot, String notes
    ) {}

    public record RedeemPointsPayload(
            @NotBlank String partyId,
            @NotNull @DecimalMin("0.01") BigDecimal points,
            String referenceType, String referenceId, String notes
    ) {}

    public record AdjustPointsPayload(
            @NotBlank String partyId,
            @NotNull BigDecimal points,
            @NotBlank String reason
    ) {}

    public record LoyaltyRuleResponse(
            BigDecimal pointsPerHundredEgp, BigDecimal redeemMaxPercent,
            int expiryMonths
    ) {}

    public record LoyaltyRulePayload(
            @NotNull @DecimalMin("0") BigDecimal pointsPerHundredEgp,
            @NotNull @DecimalMin("0") BigDecimal redeemMaxPercent,
            @NotNull @Min(1) int expiryMonths
    ) {}

    // ─── Membership ───────────────────────────────────────────

    public record MembershipPlanResponse(
            String id, String name, String nameEn, BigDecimal price,
            String currencyCode, int periodDays, int graceDays,
            boolean autoRenew, boolean active, BigDecimal loyaltyEarnRate,
            long createdAt
    ) {}

    public record MembershipPlanPayload(
            @NotBlank String name, String nameEn,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank String currencyCode,
            @NotNull @Min(1) int periodDays,
            @NotNull @Min(0) int graceDays,
            boolean autoRenew, BigDecimal loyaltyEarnRate
    ) {}

    public record MemberSubscriptionResponse(
            String id, String partyId, String planId, String planName,
            long startDate, long currentPeriodEnd, Long nextInvoiceDate,
            String status, Long cancelledAt, long createdAt
    ) {}

    public record SubscribePayload(
            @NotBlank String partyId, @NotBlank String planId
    ) {}

    // ─── Referral ─────────────────────────────────────────────

    public record ReferralResponse(
            String id, String referrerPartyId, String referredPartyId,
            String status, BigDecimal rewardPoints, String firstPurchaseReferenceId,
            long createdAt
    ) {}

    public record CreateReferralPayload(
            @NotBlank String referrerPartyId, @NotBlank String referredPartyId
    ) {}

    public record ReferralRulePayload(
            @NotNull @DecimalMin("0") BigDecimal referrerPoints,
            @NotNull @DecimalMin("0") BigDecimal referredPoints
    ) {}

    public record ReferralRuleResponse(
            BigDecimal referrerPoints, BigDecimal referredPoints
    ) {}

    public record ReferralReportEntry(
            String partyId, String partyName, long totalReferrals,
            long rewardedReferrals, BigDecimal totalPointsEarned
    ) {}
}
