package com.bemo.hr.growth.api;

import com.bemo.hr.growth.application.LoyaltyService;
import com.bemo.hr.growth.application.MembershipService;
import com.bemo.hr.growth.application.ReferralService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/growth")
@PreAuthorize("@auth.hasPermission('procurement.read')")
public class GrowthPackController {

    private final LoyaltyService loyaltyService;
    private final MembershipService membershipService;
    private final ReferralService referralService;

    public GrowthPackController(LoyaltyService loyaltyService,
                                MembershipService membershipService,
                                ReferralService referralService) {
        this.loyaltyService = loyaltyService;
        this.membershipService = membershipService;
        this.referralService = referralService;
    }

    // ─── Loyalty ──────────────────────────────────────────────

    @GetMapping("/loyalty/rules")
    public GrowthPackApi.LoyaltyRuleResponse loyaltyRules() {
        return loyaltyService.getRules();
    }

    @PutMapping("/loyalty/rules")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public void updateLoyaltyRules(@Valid @RequestBody GrowthPackApi.LoyaltyRulePayload payload) {
        loyaltyService.updateRules(payload);
    }

    @GetMapping("/loyalty/accounts/{partyId}")
    public GrowthPackApi.LoyaltyAccountResponse getLoyaltyAccount(@PathVariable String partyId) {
        return loyaltyService.getAccount(partyId);
    }

    @GetMapping("/loyalty/ledger/{partyId}")
    public List<GrowthPackApi.LoyaltyLedgerEntryResponse> getLoyaltyLedger(@PathVariable String partyId) {
        return loyaltyService.getLedger(partyId);
    }

    @PostMapping("/loyalty/earn")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.LoyaltyAccountResponse earnPoints(
            @Valid @RequestBody GrowthPackApi.EarnPointsPayload payload, Authentication auth) {
        return loyaltyService.earnPoints(payload, auth.getName());
    }

    @PostMapping("/loyalty/redeem")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.LoyaltyAccountResponse redeemPoints(
            @Valid @RequestBody GrowthPackApi.RedeemPointsPayload payload, Authentication auth) {
        return loyaltyService.redeemPoints(payload, auth.getName());
    }

    @PostMapping("/loyalty/adjust")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.LoyaltyAccountResponse adjustPoints(
            @Valid @RequestBody GrowthPackApi.AdjustPointsPayload payload, Authentication auth) {
        return loyaltyService.adjustPoints(payload, auth.getName());
    }

    @PostMapping("/loyalty/recompute/{partyId}")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.LoyaltyAccountResponse recomputeBalance(@PathVariable String partyId) {
        return loyaltyService.recomputeBalance(partyId);
    }

    @PostMapping("/loyalty/expire/{partyId}")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.LoyaltyAccountResponse expirePoints(
            @PathVariable String partyId,
            @RequestParam java.math.BigDecimal points,
            Authentication auth) {
        return loyaltyService.expirePoints(partyId, points, auth.getName());
    }

    // ─── Membership ───────────────────────────────────────────

    @GetMapping("/membership/plans")
    public List<GrowthPackApi.MembershipPlanResponse> listPlans() {
        return membershipService.listPlans();
    }

    @PostMapping("/membership/plans")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.MembershipPlanResponse createPlan(
            @Valid @RequestBody GrowthPackApi.MembershipPlanPayload payload) {
        return membershipService.createPlan(payload);
    }

    @GetMapping("/membership/subscriptions")
    public List<GrowthPackApi.MemberSubscriptionResponse> listSubscriptions(
            @RequestParam(required = false) String partyId) {
        return membershipService.listSubscriptions(partyId);
    }

    @PostMapping("/membership/subscribe")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.MemberSubscriptionResponse subscribe(
            @Valid @RequestBody GrowthPackApi.SubscribePayload payload) {
        return membershipService.subscribe(payload);
    }

    @PostMapping("/membership/subscriptions/{id}/cancel")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public void cancelSubscription(@PathVariable String id) {
        membershipService.cancelSubscription(id);
    }

    @PostMapping("/membership/renewal/run")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public Map<String, Object> runRenewal() {
        return membershipService.runRenewal();
    }

    // ─── Referral ─────────────────────────────────────────────

    @GetMapping("/referrals/rules")
    public GrowthPackApi.ReferralRuleResponse referralRules() {
        return referralService.getRules();
    }

    @PutMapping("/referrals/rules")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public void updateReferralRules(@Valid @RequestBody GrowthPackApi.ReferralRulePayload payload) {
        referralService.updateRules(payload);
    }

    @PostMapping("/referrals")
    @PreAuthorize("@auth.hasPermission('procurement.manage')")
    public GrowthPackApi.ReferralResponse createReferral(
            @Valid @RequestBody GrowthPackApi.CreateReferralPayload payload) {
        return referralService.createReferral(payload);
    }

    @GetMapping("/referrals/{referrerPartyId}")
    public List<GrowthPackApi.ReferralResponse> listReferrals(@PathVariable String referrerPartyId) {
        return referralService.listByReferrer(referrerPartyId);
    }

    @GetMapping("/referrals/report/top")
    public List<GrowthPackApi.ReferralReportEntry> topReferrers(
            @RequestParam(defaultValue = "10") int limit) {
        return referralService.topReferrers(limit);
    }
}
