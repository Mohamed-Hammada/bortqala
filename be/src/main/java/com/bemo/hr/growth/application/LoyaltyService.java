package com.bemo.hr.growth.application;

import com.bemo.hr.growth.api.GrowthPackApi;
import com.bemo.hr.growth.domain.LoyaltyAccount;
import com.bemo.hr.growth.domain.LoyaltyLedgerEntry;
import com.bemo.hr.growth.infrastructure.LoyaltyAccountRepository;
import com.bemo.hr.growth.infrastructure.LoyaltyLedgerEntryRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyLedgerEntryRepository ledgerRepository;

    private BigDecimal pointsPerHundredEgp = new BigDecimal("1.00");
    private BigDecimal redeemMaxPercent = new BigDecimal("50.00");
    private int expiryMonths = 12;

    public LoyaltyService(LoyaltyAccountRepository accountRepository,
                          LoyaltyLedgerEntryRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    public GrowthPackApi.LoyaltyRuleResponse getRules() {
        return new GrowthPackApi.LoyaltyRuleResponse(pointsPerHundredEgp, redeemMaxPercent, expiryMonths);
    }

    @Transactional
    public void updateRules(GrowthPackApi.LoyaltyRulePayload payload) {
        this.pointsPerHundredEgp = payload.pointsPerHundredEgp();
        this.redeemMaxPercent = payload.redeemMaxPercent();
        this.expiryMonths = payload.expiryMonths();
    }

    public GrowthPackApi.LoyaltyAccountResponse getAccount(String partyId) {
        String appId = TenantContext.require();
        LoyaltyAccount account = accountRepository.findByAppIdAndPartyId(appId, partyId)
                .orElseGet(() -> createAccount(appId, partyId));
        return toAccountResponse(account);
    }

    public List<GrowthPackApi.LoyaltyLedgerEntryResponse> getLedger(String partyId) {
        String appId = TenantContext.require();
        Optional<LoyaltyAccount> account = accountRepository.findByAppIdAndPartyId(appId, partyId);
        if (account.isEmpty()) return List.of();
        return ledgerRepository.findByLoyaltyAccountIdOrderByCreatedAtDesc(account.get().getId())
                .stream().map(this::toLedgerResponse).toList();
    }

    @Transactional
    public GrowthPackApi.LoyaltyAccountResponse earnPoints(GrowthPackApi.EarnPointsPayload payload, String actor) {
        String appId = TenantContext.require();
        LoyaltyAccount account = accountRepository.findByAppIdAndPartyId(appId, payload.partyId())
                .orElseGet(() -> createAccount(appId, payload.partyId()));

        account.credit(payload.points());
        LoyaltyAccount saved = accountRepository.save(account);

        LoyaltyLedgerEntry entry = new LoyaltyLedgerEntry(
                saved.getId(), payload.partyId(), "EARN", payload.points(),
                saved.getPointsBalance(), payload.referenceType(), payload.referenceId(),
                payload.ruleSnapshot(), payload.notes(), actor);
        ledgerRepository.save(entry);

        return toAccountResponse(saved);
    }

    @Transactional
    public GrowthPackApi.LoyaltyAccountResponse redeemPoints(GrowthPackApi.RedeemPointsPayload payload, String actor) {
        String appId = TenantContext.require();
        LoyaltyAccount account = accountRepository.findByAppIdAndPartyId(appId, payload.partyId())
                .orElseThrow(() -> new BusinessRuleException("Loyalty account not found.", "LOYALTY_ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (account.getPointsBalance().compareTo(payload.points()) < 0) {
            throw new BusinessRuleException(
                    "Insufficient points balance. Available: " + account.getPointsBalance(),
                    "LOYALTY_INSUFFICIENT_POINTS", HttpStatus.CONFLICT);
        }

        account.debit(payload.points());
        LoyaltyAccount saved = accountRepository.save(account);

        LoyaltyLedgerEntry entry = new LoyaltyLedgerEntry(
                saved.getId(), payload.partyId(), "REDEEM", payload.points(),
                saved.getPointsBalance(), payload.referenceType(), payload.referenceId(),
                null, payload.notes(), actor);
        ledgerRepository.save(entry);

        return toAccountResponse(saved);
    }

    @Transactional
    public GrowthPackApi.LoyaltyAccountResponse adjustPoints(GrowthPackApi.AdjustPointsPayload payload, String actor) {
        String appId = TenantContext.require();
        LoyaltyAccount account = accountRepository.findByAppIdAndPartyId(appId, payload.partyId())
                .orElseGet(() -> createAccount(appId, payload.partyId()));

        String type = payload.points().compareTo(BigDecimal.ZERO) >= 0 ? "EARN" : "REDEEM";
        BigDecimal absPoints = payload.points().abs();

        if ("REDEEM".equals(type) && account.getPointsBalance().compareTo(absPoints) < 0) {
            throw new BusinessRuleException("Cannot adjust: would result in negative balance.", "LOYALTY_ADJUST_NEGATIVE", HttpStatus.CONFLICT);
        }

        if ("EARN".equals(type)) account.credit(absPoints);
        else account.debit(absPoints);

        LoyaltyAccount saved = accountRepository.save(account);

        LoyaltyLedgerEntry entry = new LoyaltyLedgerEntry(
                saved.getId(), payload.partyId(), "ADJUST", payload.points(),
                saved.getPointsBalance(), "MANUAL", null,
                null, "Admin adjustment: " + payload.reason(), actor);
        ledgerRepository.save(entry);

        return toAccountResponse(saved);
    }

    /** Calculate how many points an EGP spend earns. */
    public BigDecimal calculateEarnPoints(BigDecimal spendAmount) {
        if (pointsPerHundredEgp.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return spendAmount.multiply(pointsPerHundredEgp)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /** Calculate the max redeem discount for a given sale amount. */
    public BigDecimal calculateRedeemDiscount(BigDecimal saleAmount, BigDecimal pointsBalance, BigDecimal pointValue) {
        if (redeemMaxPercent.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal maxDiscount = saleAmount.multiply(redeemMaxPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal pointsValue = pointsBalance.multiply(pointValue);
        return pointsValue.min(maxDiscount);
    }

    private LoyaltyAccount createAccount(String appId, String partyId) {
        LoyaltyAccount account = new LoyaltyAccount(partyId);
        account.setAppId(appId);
        return accountRepository.save(account);
    }

    private GrowthPackApi.LoyaltyAccountResponse toAccountResponse(LoyaltyAccount a) {
        return new GrowthPackApi.LoyaltyAccountResponse(
                a.getId(), a.getPartyId(), a.getPointsBalance(),
                a.getTotalEarned(), a.getTotalRedeemed(), a.getTotalExpired(), a.getCreatedAt());
    }

    private GrowthPackApi.LoyaltyLedgerEntryResponse toLedgerResponse(LoyaltyLedgerEntry e) {
        return new GrowthPackApi.LoyaltyLedgerEntryResponse(
                e.getId(), e.getLoyaltyAccountId(), e.getPartyId(), e.getType(),
                e.getPoints(), e.getRunningBalance(), e.getReferenceType(), e.getReferenceId(),
                e.getRuleSnapshot(), e.getNotes(), e.getActor(), e.getCreatedAt());
    }
}
