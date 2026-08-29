package com.bemo.hr.growth;

import com.bemo.hr.growth.api.GrowthPackApi;
import com.bemo.hr.growth.application.LoyaltyService;
import com.bemo.hr.growth.application.MembershipService;
import com.bemo.hr.growth.application.ReferralService;
import com.bemo.hr.growth.domain.LoyaltyAccount;
import com.bemo.hr.growth.domain.LoyaltyLedgerEntry;
import com.bemo.hr.growth.domain.MembershipPlan;
import com.bemo.hr.growth.domain.MemberSubscription;
import com.bemo.hr.growth.domain.Referral;
import com.bemo.hr.growth.infrastructure.*;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthPackServiceTests {

    @Mock LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock LoyaltyLedgerEntryRepository loyaltyLedgerEntryRepository;
    @Mock MembershipPlanRepository membershipPlanRepository;
    @Mock MemberSubscriptionRepository memberSubscriptionRepository;
    @Mock ReferralRepository referralRepository;

    LoyaltyService loyaltyService;
    MembershipService membershipService;
    ReferralService referralService;

    @BeforeEach
    void setUp() {
        TenantContext.set("test-app");
        loyaltyService = new LoyaltyService(loyaltyAccountRepository, loyaltyLedgerEntryRepository);
        membershipService = new MembershipService(membershipPlanRepository, memberSubscriptionRepository);
        referralService = new ReferralService(referralRepository, loyaltyService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ─── Loyalty tests ─────────────────────────────────────────

    @Test
    void earnPointsCreatesAccountIfMissing() {
        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "p1")).thenReturn(Optional.empty());
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyLedgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = loyaltyService.earnPoints(
                new GrowthPackApi.EarnPointsPayload("p1", new BigDecimal("10"), "SALE", "sale-1", null, null),
                "test-user");

        verify(loyaltyAccountRepository, atLeastOnce()).save(any(LoyaltyAccount.class));
        verify(loyaltyLedgerEntryRepository, times(1)).save(any(LoyaltyLedgerEntry.class));
        assertThat(result.pointsBalance()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void redeemInsufficientPointsThrows() {
        LoyaltyAccount account = new LoyaltyAccount("p1");
        account.setAppId("test-app");
        account.credit(new BigDecimal("5"));

        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "p1")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loyaltyService.redeemPoints(
                new GrowthPackApi.RedeemPointsPayload("p1", new BigDecimal("100"), "SALE", "s1", null), "test-user"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("LOYALTY_INSUFFICIENT_POINTS"));
    }

    @Test
    void calculateEarnPointsRespectsRule() {
        BigDecimal points = loyaltyService.calculateEarnPoints(new BigDecimal("500"));
        assertThat(points).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void adjustPointsDebitFailsOnNegativeBalance() {
        LoyaltyAccount account = new LoyaltyAccount("p1");
        account.setAppId("test-app");
        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "p1")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loyaltyService.adjustPoints(
                new GrowthPackApi.AdjustPointsPayload("p1", new BigDecimal("-100"), "Correction"), "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("LOYALTY_ADJUST_NEGATIVE"));
    }

    @Test
    void expirePoints_deductsPointsAndRecordsLedgerEntry() {
        LoyaltyAccount account = new LoyaltyAccount("p1");
        account.setAppId("test-app");
        account.credit(new BigDecimal("100"));
        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "p1")).thenReturn(Optional.of(account));
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyLedgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = loyaltyService.expirePoints("p1", new BigDecimal("30"), "EXPIRY_CRON");

        assertThat(result.pointsBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(result.totalExpired()).isEqualByComparingTo(new BigDecimal("30.00"));
        verify(loyaltyLedgerEntryRepository).save(any(LoyaltyLedgerEntry.class));
    }

    @Test
    void recomputeBalance_derivesFromLedger() {
        LoyaltyAccount account = new LoyaltyAccount("p1");
        account.setAppId("test-app");
        account.credit(new BigDecimal("50"));
        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "p1")).thenReturn(Optional.of(account));
        when(loyaltyAccountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        var entry1 = new LoyaltyLedgerEntry(account.getId(), "p1", "EARN", new BigDecimal("100"),
                new BigDecimal("100"), "SALE", "s1", null, null, "sys");
        var entry2 = new LoyaltyLedgerEntry(account.getId(), "p1", "REDEEM", new BigDecimal("40"),
                new BigDecimal("60"), "SALE", "s2", null, null, "sys");
        when(loyaltyLedgerEntryRepository.findByLoyaltyAccountIdOrderByCreatedAtDesc(account.getId()))
                .thenReturn(List.of(entry1, entry2));

        var result = loyaltyService.recomputeBalance("p1");

        assertThat(result.pointsBalance()).isNotNull();
    }

    // ─── Membership tests ──────────────────────────────────────

    @Test
    void createPlanSuccess() {
        when(membershipPlanRepository.save(any(MembershipPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = membershipService.createPlan(new GrowthPackApi.MembershipPlanPayload(
                "Gold", "Gold Plan", new BigDecimal("299.00"), "EGP", 30, 7, true, null));

        assertThat(result.name()).isEqualTo("Gold");
        verify(membershipPlanRepository).save(any(MembershipPlan.class));
    }

    @Test
    void subscribePlanNotFoundThrows() {
        when(membershipPlanRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> membershipService.subscribe(
                new GrowthPackApi.SubscribePayload("p1", "missing")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("MEMBERSHIP_PLAN_NOT_FOUND"));
    }

    @Test
    void cancelSubscriptionAlreadyCancelledThrows() {
        MemberSubscription sub = new MemberSubscription("p1", "plan1", System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, "CANCELLED");
        when(memberSubscriptionRepository.findById("sub1")).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> membershipService.cancelSubscription("sub1"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("MEMBERSHIP_SUB_INVALID_STATE"));
    }

    // ─── Referral tests ────────────────────────────────────────

    @Test
    void createReferralDuplicateThrows() {
        when(referralRepository.existsByAppIdAndReferredPartyId("test-app", "p2")).thenReturn(true);

        assertThatThrownBy(() -> referralService.createReferral(
                new GrowthPackApi.CreateReferralPayload("p1", "p2")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("REFERRAL_DUPLICATE"));
    }

    @Test
    void createReferralSelfReferralThrows() {
        when(referralRepository.existsByAppIdAndReferredPartyId("test-app", "p1")).thenReturn(false);

        assertThatThrownBy(() -> referralService.createReferral(
                new GrowthPackApi.CreateReferralPayload("p1", "p1")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("REFERRAL_SELF"));
    }

    @Test
    void onFirstPurchaseRewardsBothSides() {
        Referral referral = new Referral("referrer1", "referred1");
        referral.setAppId("test-app");
        when(referralRepository.findByAppIdAndReferredPartyId("test-app", "referred1")).thenReturn(Optional.of(referral));
        when(referralRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoyaltyAccount referrerAcct = new LoyaltyAccount("referrer1");
        referrerAcct.setAppId("test-app");
        referrerAcct.credit(new BigDecimal("100"));
        LoyaltyAccount referredAcct = new LoyaltyAccount("referred1");
        referredAcct.setAppId("test-app");
        referredAcct.credit(new BigDecimal("50"));

        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "referrer1")).thenReturn(Optional.of(referrerAcct));
        when(loyaltyAccountRepository.findByAppIdAndPartyId("test-app", "referred1")).thenReturn(Optional.of(referredAcct));
        when(loyaltyAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loyaltyLedgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        referralService.onFirstPurchase("referred1", "sale-1");

        verify(referralRepository, times(2)).save(any());
        assertThat(referral.getStatus()).isEqualTo("REWARDED");
    }

    @Test
    void loyaltyAccountBalanceNeverGoesNegative() {
        LoyaltyAccount account = new LoyaltyAccount("p1");
        account.setAppId("test-app");
        account.credit(new BigDecimal("10"));
        account.debit(new BigDecimal("3"));
        assertThat(account.getPointsBalance()).isEqualByComparingTo(new BigDecimal("7"));

        assertThatThrownBy(() -> account.debit(new BigDecimal("100")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(account.getPointsBalance()).isEqualByComparingTo(new BigDecimal("7"));
    }
}
