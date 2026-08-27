package com.bemo.hr.growth.application;

import com.bemo.hr.growth.api.GrowthPackApi;
import com.bemo.hr.growth.domain.MembershipPlan;
import com.bemo.hr.growth.domain.MemberSubscription;
import com.bemo.hr.growth.infrastructure.MembershipPlanRepository;
import com.bemo.hr.growth.infrastructure.MemberSubscriptionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MembershipService {

    private final MembershipPlanRepository planRepository;
    private final MemberSubscriptionRepository subscriptionRepository;

    public MembershipService(MembershipPlanRepository planRepository,
                             MemberSubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<GrowthPackApi.MembershipPlanResponse> listPlans() {
        String appId = TenantContext.require();
        return planRepository.findByAppIdAndActiveTrue(appId)
                .stream().map(this::toPlanResponse).toList();
    }

    @Transactional
    public GrowthPackApi.MembershipPlanResponse createPlan(GrowthPackApi.MembershipPlanPayload payload) {
        String appId = TenantContext.require();
        MembershipPlan plan = new MembershipPlan(
                payload.name(), payload.nameEn(), payload.price(), payload.currencyCode(),
                payload.periodDays(), payload.graceDays(), payload.autoRenew(), payload.loyaltyEarnRate());
        plan.setAppId(appId);
        return toPlanResponse(planRepository.save(plan));
    }

    public List<GrowthPackApi.MemberSubscriptionResponse> listSubscriptions(String partyId) {
        String appId = TenantContext.require();
        List<MemberSubscription> subs = partyId != null
                ? subscriptionRepository.findByAppIdAndPartyId(appId, partyId)
                : subscriptionRepository.findByAppIdAndStatusIn(appId, List.of("ACTIVE", "GRACE"));
        return subs.stream().map(this::toSubResponse).toList();
    }

    @Transactional
    public GrowthPackApi.MemberSubscriptionResponse subscribe(GrowthPackApi.SubscribePayload payload) {
        String appId = TenantContext.require();
        MembershipPlan plan = planRepository.findById(payload.planId())
                .orElseThrow(() -> new BusinessRuleException("Membership plan not found.", "MEMBERSHIP_PLAN_NOT_FOUND", HttpStatus.NOT_FOUND));

        long now = System.currentTimeMillis();
        long periodEnd = now + (long) plan.getPeriodDays() * 86400000L;
        MemberSubscription sub = new MemberSubscription(payload.partyId(), payload.planId(), now, periodEnd, "ACTIVE");
        sub.setAppId(appId);
        return toSubResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public void cancelSubscription(String subscriptionId) {
        MemberSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessRuleException("Subscription not found.", "MEMBERSHIP_SUB_NOT_FOUND", HttpStatus.NOT_FOUND));
        if ("CANCELLED".equals(sub.getStatus()) || "EXPIRED".equals(sub.getStatus())) {
            throw new BusinessRuleException("Subscription is already " + sub.getStatus(), "MEMBERSHIP_SUB_INVALID_STATE", HttpStatus.CONFLICT);
        }
        sub.cancel();
        subscriptionRepository.save(sub);
    }

    /** Renewal job: create invoices for subscriptions in grace or past period end. */
    @Transactional
    public Map<String, Object> runRenewal() {
        long now = System.currentTimeMillis();
        String appId = TenantContext.require();
        List<MemberSubscription> expired = subscriptionRepository
                .findByAppIdAndStatusAndCurrentPeriodEndLessThanEqual(appId, "ACTIVE", now);
        int graceCount = 0;
        int expiredCount = 0;
        for (MemberSubscription sub : expired) {
            MembershipPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
            if (plan == null || !plan.isAutoRenew()) {
                sub.setStatus("EXPIRED");
                subscriptionRepository.save(sub);
                expiredCount++;
                continue;
            }
            long graceDeadline = now + (long) plan.getGraceDays() * 86400000L;
            sub.enterGrace(graceDeadline);
            sub.setNextInvoiceDate(now);
            subscriptionRepository.save(sub);
            graceCount++;
        }

        List<MemberSubscription> graceExpired = subscriptionRepository
                .findByAppIdAndStatusAndCurrentPeriodEndLessThanEqual(appId, "GRACE", now);
        for (MemberSubscription sub : graceExpired) {
            sub.expire();
            subscriptionRepository.save(sub);
            expiredCount++;
        }
        return Map.of("graceCount", graceCount, "expiredCount", expiredCount);
    }

    public boolean isActiveForMember(String partyId) {
        String appId = TenantContext.require();
        return subscriptionRepository.findByAppIdAndPartyIdAndStatusIn(appId, partyId, List.of("ACTIVE", "GRACE")).isPresent();
    }

    private GrowthPackApi.MembershipPlanResponse toPlanResponse(MembershipPlan p) {
        return new GrowthPackApi.MembershipPlanResponse(
                p.getId(), p.getName(), p.getNameEn(), p.getPrice(),
                p.getCurrencyCode(), p.getPeriodDays(), p.getGraceDays(),
                p.isAutoRenew(), p.isActive(), p.getLoyaltyEarnRate(), p.getCreatedAt());
    }

    private GrowthPackApi.MemberSubscriptionResponse toSubResponse(MemberSubscription s) {
        return new GrowthPackApi.MemberSubscriptionResponse(
                s.getId(), s.getPartyId(), s.getPlanId(), null,
                s.getStartDate(), s.getCurrentPeriodEnd(), s.getNextInvoiceDate(),
                s.getStatus(), s.getCancelledAt(), s.getCreatedAt());
    }
}
