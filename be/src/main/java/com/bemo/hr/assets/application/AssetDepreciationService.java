package com.bemo.hr.assets.application;

import com.bemo.hr.assets.api.AssetsApi;
import com.bemo.hr.assets.domain.FixedAsset;
import com.bemo.hr.assets.domain.FixedAssetDepreciationPost;
import com.bemo.hr.assets.infrastructure.FixedAssetDepreciationPostRepository;
import com.bemo.hr.assets.infrastructure.FixedAssetRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.finance.api.AccountingApi;
import com.bemo.hr.finance.application.JournalEntryService;
import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WP-04: month-end depreciation run and disposal journals.
 *
 * <p>One balanced journal per asset per month (Dr depreciation expense /
 * Cr accumulated depreciation), guarded by the unique (app_id, asset_id, year_month)
 * evidence row so double runs post exactly once. A missing chart-of-accounts account
 * or a locked fiscal period skips the individual asset — it never fails the whole run.
 */
@Slf4j
@Service
public class AssetDepreciationService {

    private final FixedAssetRepository assetRepository;
    private final FixedAssetDepreciationPostRepository postRepository;
    private final JournalEntryService journalEntryService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final FiscalPeriodGuard fiscalPeriodGuard;
    private final AuditService auditService;
    private final String expenseAccountCode;
    private final String accumulatedAccountCode;
    private final String assetCostAccountCode;
    private final String proceedsAccountCode;
    private final String lossAccountCode;
    private final String gainAccountCode;

    /** Serializes manual runs against the monthly scheduler. */
    private final ReentrantLock runLock = new ReentrantLock();

    public AssetDepreciationService(FixedAssetRepository assetRepository,
                                    FixedAssetDepreciationPostRepository postRepository,
                                    JournalEntryService journalEntryService,
                                    JournalEntryRepository journalEntryRepository,
                                    AccountRepository accountRepository,
                                    FiscalPeriodGuard fiscalPeriodGuard,
                                    AuditService auditService,
                                    @Value("${hr.finance.depreciation-expense-account-code:5300}") String expenseAccountCode,
                                    @Value("${hr.finance.accumulated-depreciation-account-code:1280}") String accumulatedAccountCode,
                                    @Value("${hr.finance.fixed-asset-cost-account-code:1300}") String assetCostAccountCode,
                                    @Value("${hr.finance.asset-disposal-proceeds-account-code:10101}") String proceedsAccountCode,
                                    @Value("${hr.finance.asset-disposal-loss-account-code:5310}") String lossAccountCode,
                                    @Value("${hr.finance.asset-disposal-gain-account-code:4100}") String gainAccountCode) {
        this.assetRepository = assetRepository;
        this.postRepository = postRepository;
        this.journalEntryService = journalEntryService;
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
        this.fiscalPeriodGuard = fiscalPeriodGuard;
        this.auditService = auditService;
        this.expenseAccountCode = expenseAccountCode.strip();
        this.accumulatedAccountCode = accumulatedAccountCode.strip();
        this.assetCostAccountCode = assetCostAccountCode.strip();
        this.proceedsAccountCode = proceedsAccountCode.strip();
        this.lossAccountCode = lossAccountCode.strip();
        this.gainAccountCode = gainAccountCode.strip();
    }

    public boolean tryLock() {
        return runLock.tryLock();
    }

    public void unlock() {
        runLock.unlock();
    }

    @Transactional
    public AssetsApi.DepreciationRunResponse runDepreciation(String yearMonthText, String actor) {
        YearMonth yearMonth = YearMonth.parse(yearMonthText.strip());
        List<FixedAsset> scope = assetRepository.findByStatusInOrderByAcquisitionDateAsc(
                List.of(FixedAsset.Status.ACTIVE.name()));
        List<AssetsApi.DepreciationRunResult> results = new ArrayList<>();
        int postedCount = 0;
        BigDecimal totalCharge = BigDecimal.ZERO;

        for (FixedAsset asset : scope) {
            BigDecimal charge = asset.chargeFor(yearMonth);
            if (charge.signum() <= 0) continue;

            Optional<Account> expenseAccount = findAccount(expenseAccountCode);
            Optional<Account> accumulatedAccount = findAccount(accumulatedAccountCode);
            if (expenseAccount.isEmpty() || accumulatedAccount.isEmpty()) {
                log.warn("Depreciation skipped for asset {}: missing accounts {} / {}",
                        asset.getId(), expenseAccountCode, accumulatedAccountCode);
                results.add(new AssetsApi.DepreciationRunResult(asset.getId(), asset.getName(), charge,
                        "SKIPPED_MISSING_ACCOUNT", null));
                continue;
            }

            long postingDate = endOfMonthUtc(yearMonth);
            try {
                fiscalPeriodGuard.requireOpen(Instant.ofEpochMilli(postingDate).atZone(ZoneOffset.UTC).toLocalDate());
            } catch (Exception periodException) {
                log.info("Depreciation skipped for asset {}: fiscal period locked for {}", asset.getId(), yearMonth);
                results.add(new AssetsApi.DepreciationRunResult(asset.getId(), asset.getName(), charge,
                        "DEPRECIATION_PERIOD_LOCKED", null));
                continue;
            }

            // Idempotency guard — unique (app_id, asset_id, year_month) backs this up at the DB level.
            if (postRepository.existsByAssetIdAndYearMonth(asset.getId(), yearMonth.toString())) {
                results.add(new AssetsApi.DepreciationRunResult(asset.getId(), asset.getName(), charge,
                        "ALREADY_POSTED", null));
                continue;
            }

            AccountingApi.JournalEntryPayload payload = new AccountingApi.JournalEntryPayload(
                    null, postingDate,
                    "Depreciation " + yearMonth + " — " + asset.getName(),
                    "ASSET-DEP-" + yearMonth, null, null, List.of(
                    new AccountingApi.JournalEntryLinePayload(expenseAccount.get().getId(), null,
                            charge, null, "Depreciation — " + asset.getName(),
                            asset.getCostCenterId(), null, null),
                    new AccountingApi.JournalEntryLinePayload(accumulatedAccount.get().getId(), null,
                            null, charge, "Accumulated depreciation — " + asset.getName(),
                            asset.getCostCenterId(), null, null)));
            AccountingApi.JournalEntryResponse created = journalEntryService.create(payload, actor);

            var entry = journalEntryRepository.findById(created.id()).orElseThrow();
            entry.approve(actor);
            entry.post(actor);
            journalEntryRepository.save(entry);

            postRepository.save(new FixedAssetDepreciationPost(asset.getId(), yearMonth.toString(),
                    charge, created.id()));
            asset.registerPostedCharge(yearMonth, charge);
            assetRepository.save(asset);
            auditService.record("CREATE", "FIXED_ASSET_DEPRECIATION", asset.getId(), actor,
                    "{\"yearMonth\":\"" + yearMonth + "\",\"amount\":" + charge
                            + ",\"journalEntryId\":\"" + created.id() + "\"}", null);

            postedCount += 1;
            totalCharge = totalCharge.add(charge);
            results.add(new AssetsApi.DepreciationRunResult(asset.getId(), asset.getName(), charge, "POSTED",
                    created.entryNumber()));
        }
        log.info("Depreciation run {}: {} posted of {} assets by {}", yearMonth, postedCount, results.size(), actor);
        return new AssetsApi.DepreciationRunResponse(yearMonth.toString(), postedCount, results.size(),
                totalCharge, results);
    }

    /** Disposal journal: Dr accumulated + Dr proceeds + plug loss/gain, Cr original cost. */
    @Transactional
    public AssetsApi.DisposalJournalSummary disposeWithJournal(FixedAsset asset, long disposalEpochMilli,
                                                             BigDecimal proceeds, String actor) {
        requireAccount(accumulatedAccountCode);
        requireAccount(proceedsAccountCode);
        requireAccount(assetCostAccountCode);

        BigDecimal accumulated = asset.getAccumulatedDepreciation();
        BigDecimal netBookValue = asset.netBookValue();
        BigDecimal gainOrLoss = proceeds.subtract(netBookValue);

        List<AccountingApi.JournalEntryLinePayload> lines = new ArrayList<>();
        if (accumulated.signum() > 0) {
            lines.add(new AccountingApi.JournalEntryLinePayload(accountByCode(accumulatedAccountCode).getId(),
                    null, accumulated, null, "Accumulated depreciation on disposal — " + asset.getName(),
                    asset.getCostCenterId(), null, null));
        }
        if (proceeds.signum() > 0) {
            lines.add(new AccountingApi.JournalEntryLinePayload(accountByCode(proceedsAccountCode).getId(),
                    null, proceeds, null, "Disposal proceeds — " + asset.getName(),
                    asset.getCostCenterId(), null, null));
        }
        if (gainOrLoss.signum() < 0) {
            lines.add(new AccountingApi.JournalEntryLinePayload(accountByCode(lossAccountCode).getId(),
                    null, gainOrLoss.abs(), null, "Loss on disposal — " + asset.getName(),
                    asset.getCostCenterId(), null, null));
        }
        lines.add(new AccountingApi.JournalEntryLinePayload(accountByCode(assetCostAccountCode).getId(),
                null, null, asset.getAcquisitionCost(), "Cost write-off — " + asset.getName(),
                asset.getCostCenterId(), null, null));
        if (gainOrLoss.signum() > 0) {
            lines.add(new AccountingApi.JournalEntryLinePayload(accountByCode(gainAccountCode).getId(),
                    null, null, gainOrLoss, "Gain on disposal — " + asset.getName(),
                    asset.getCostCenterId(), null, null));
        }

        AccountingApi.JournalEntryPayload payload = new AccountingApi.JournalEntryPayload(
                null, disposalEpochMilli, "Disposal — " + asset.getName(),
                "ASSET-DISPOSAL-" + asset.getId().substring(0, 8), null, null, lines);
        AccountingApi.JournalEntryResponse created = journalEntryService.create(payload, actor);
        var entry = journalEntryRepository.findById(created.id()).orElseThrow();
        entry.approve(actor);
        entry.post(actor);
        journalEntryRepository.save(entry);

        auditService.record("DISPOSE", "FIXED_ASSET", asset.getId(), actor,
                "{\"proceeds\":" + proceeds + ",\"gainOrLoss\":" + gainOrLoss
                        + ",\"journalEntryId\":\"" + created.id() + "\"}", null);

        return new AssetsApi.DisposalJournalSummary(created.id(), created.entryNumber(),
                netBookValue, gainOrLoss);
    }

    private void requireAccount(String code) {
        if (findAccount(code).isEmpty())
            throw new com.bemo.hr.shared.domain.BusinessRuleException(
                    "Account " + code + " is missing from the chart of accounts.",
                    "ASSET_DISPOSAL_INVALID", org.springframework.http.HttpStatus.CONFLICT);
    }

    private Account accountByCode(String code) {
        return findAccount(code).orElseThrow();
    }

    private Optional<Account> findAccount(String code) {
        return accountRepository.findAll().stream()
                .filter(account -> account.getCode().equals(code))
                .findFirst();
    }

    private static long endOfMonthUtc(YearMonth yearMonth) {
        return yearMonth.atEndOfMonth().atTime(23, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
