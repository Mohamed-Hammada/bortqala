package com.bemo.hr.growth.application;

import com.bemo.hr.growth.api.GrowthPackApi;
import com.bemo.hr.growth.domain.Referral;
import com.bemo.hr.growth.infrastructure.ReferralRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final LoyaltyService loyaltyService;

    private BigDecimal referrerPoints = new BigDecimal("100.00");
    private BigDecimal referredPoints = new BigDecimal("50.00");

    public ReferralService(ReferralRepository referralRepository, LoyaltyService loyaltyService) {
        this.referralRepository = referralRepository;
        this.loyaltyService = loyaltyService;
    }

    public GrowthPackApi.ReferralRuleResponse getRules() {
        return new GrowthPackApi.ReferralRuleResponse(referrerPoints, referredPoints);
    }

    @Transactional
    public void updateRules(GrowthPackApi.ReferralRulePayload payload) {
        this.referrerPoints = payload.referrerPoints();
        this.referredPoints = payload.referredPoints();
    }

    @Transactional
    public GrowthPackApi.ReferralResponse createReferral(GrowthPackApi.CreateReferralPayload payload) {
        String appId = TenantContext.require();
        if (referralRepository.existsByAppIdAndReferredPartyId(appId, payload.referredPartyId())) {
            throw new BusinessRuleException("This customer has already been referred.", "REFERRAL_DUPLICATE", HttpStatus.CONFLICT);
        }
        if (payload.referrerPartyId().equals(payload.referredPartyId())) {
            throw new BusinessRuleException("A customer cannot refer themselves.", "REFERRAL_SELF", HttpStatus.CONFLICT);
        }

        Referral referral = new Referral(payload.referrerPartyId(), payload.referredPartyId());
        referral.setAppId(appId);
        return toResponse(referralRepository.save(referral));
    }

    /** Called by sales posting when a referred customer makes their first purchase. */
    @Transactional
    public void onFirstPurchase(String referredPartyId, String salesReferenceId) {
        String appId = TenantContext.require();
        Referral referral = referralRepository.findByAppIdAndReferredPartyId(appId, referredPartyId)
                .orElse(null);
        if (referral == null || !"REGISTERED".equals(referral.getStatus())) return;

        BigDecimal totalReward = referrerPoints.add(referredPoints);
        referral.markFirstPurchase(salesReferenceId, totalReward);
        referralRepository.save(referral);

        loyaltyService.earnPoints(new GrowthPackApi.EarnPointsPayload(
                referral.getReferrerPartyId(), referrerPoints, "REFERRAL", referral.getId(),
                "Referrer reward for referring customer", null), "SYSTEM");
        loyaltyService.earnPoints(new GrowthPackApi.EarnPointsPayload(
                referredPartyId, referredPoints, "REFERRAL", referral.getId(),
                "Welcome referral bonus", null), "SYSTEM");

        referral.markRewarded();
        referralRepository.save(referral);
    }

    public List<GrowthPackApi.ReferralResponse> listByReferrer(String referrerPartyId) {
        String appId = TenantContext.require();
        return referralRepository.findByAppIdAndReferrerPartyIdOrderByCreatedAtDesc(appId, referrerPartyId)
                .stream().map(this::toResponse).toList();
    }

    public List<GrowthPackApi.ReferralReportEntry> topReferrers(int limit) {
        String appId = TenantContext.require();
        // Simple implementation: list all referrals and aggregate in-memory
        List<Referral> all = referralRepository.findAll();
        var grouped = all.stream()
                .filter(r -> appId.equals(r.getAppId()))
                .collect(java.util.stream.Collectors.groupingBy(Referral::getReferrerPartyId));
        return grouped.entrySet().stream()
                .map(e -> {
                    List<Referral> refs = e.getValue();
                    long total = refs.size();
                    long rewarded = refs.stream().filter(r -> "REWARDED".equals(r.getStatus())).count();
                    BigDecimal totalPts = refs.stream()
                            .map(r -> r.getRewardPoints() != null ? r.getRewardPoints() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new GrowthPackApi.ReferralReportEntry(e.getKey(), e.getKey(), total, rewarded, totalPts);
                })
                .sorted((a, b) -> Long.compare(b.totalReferrals(), a.totalReferrals()))
                .limit(limit)
                .toList();
    }

    private GrowthPackApi.ReferralResponse toResponse(Referral r) {
        return new GrowthPackApi.ReferralResponse(
                r.getId(), r.getReferrerPartyId(), r.getReferredPartyId(),
                r.getStatus(), r.getRewardPoints(), r.getFirstPurchaseReferenceId(), r.getCreatedAt());
    }
}
