package com.bemo.hr.finance.application;

import com.bemo.hr.finance.api.AccountingApi;
import com.bemo.hr.finance.api.FxRevaluationApi;
import com.bemo.hr.finance.domain.Account;
import com.bemo.hr.finance.domain.Currency;
import com.bemo.hr.finance.domain.FiscalPeriodGuard;
import com.bemo.hr.finance.domain.FxRevaluationPost;
import com.bemo.hr.finance.domain.JournalEntry;
import com.bemo.hr.finance.domain.JournalEntryLine;
import com.bemo.hr.finance.infrastructure.AccountRepository;
import com.bemo.hr.finance.infrastructure.CurrencyRepository;
import com.bemo.hr.finance.infrastructure.FxRevaluationPostRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryLineRepository;
import com.bemo.hr.finance.infrastructure.JournalEntryRepository;
import com.bemo.hr.shared.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FxRevaluationService {

    private final FxRevaluationPostRepository fxRevaluationPostRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountRepository accountRepository;
    private final CurrencyRepository currencyRepository;
    private final JournalEntryService journalEntryService;
    private final FiscalPeriodGuard fiscalPeriodGuard;

    @Value("${hr.finance.fx-revaluation-gain-account-code:4200}")
    private String fxGainAccountCode = "4200";

    @Value("${hr.finance.fx-revaluation-loss-account-code:4200}")
    private String fxLossAccountCode = "4200";

    @Value("${hr.finance.fx-revaluation-holding-account-code:10101}")
    private String fxHoldingAccountCode = "10101";

    public FxRevaluationService(FxRevaluationPostRepository fxRevaluationPostRepository,
                                JournalEntryRepository journalEntryRepository,
                                JournalEntryLineRepository journalEntryLineRepository,
                                AccountRepository accountRepository,
                                CurrencyRepository currencyRepository,
                                JournalEntryService journalEntryService,
                                FiscalPeriodGuard fiscalPeriodGuard) {
        this.fxRevaluationPostRepository = fxRevaluationPostRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.accountRepository = accountRepository;
        this.currencyRepository = currencyRepository;
        this.journalEntryService = journalEntryService;
        this.fiscalPeriodGuard = fiscalPeriodGuard;
    }

    @Transactional
    public FxRevaluationApi.RevaluationRunResponse runRevaluation(LocalDate asOf, String username) {
        String appId = TenantContext.require();
        String yearMonth = YearMonth.from(asOf).toString();

        log.info("FX revaluation run for asOf={}, yearMonth={}, user={}", asOf, yearMonth, username);

        List<Account> allAccounts = accountRepository.findAllByOrderByCodeAsc();

        List<JournalEntry> postedEntries = journalEntryRepository
                .findByStatusInOrderByEntryDateDesc(List.of(JournalEntry.Status.POSTED));

        Set<String> nonEgpAccountIds = allAccounts.stream()
                .filter(a -> !a.isHeader() && a.isActive())
                .filter(a -> !("EGP".equalsIgnoreCase(a.getCurrency())))
                .map(Account::getId)
                .collect(Collectors.toSet());

        Map<String, BigDecimal> netBalanceByCurrency = new HashMap<>();
        Set<String> entryIds = postedEntries.stream().map(JournalEntry::getId).collect(Collectors.toSet());

        if (!entryIds.isEmpty()) {
            List<JournalEntryLine> lines = journalEntryLineRepository.findByJournalEntryIdIn(entryIds);

            Map<String, JournalEntry> entryMap = postedEntries.stream()
                    .collect(Collectors.toMap(JournalEntry::getId, e -> e));

            for (JournalEntryLine line : lines) {
                if (!nonEgpAccountIds.contains(line.getAccountId())) continue;

                JournalEntry entry = entryMap.get(line.getJournalEntryId());
                if (entry == null) continue;

                String currency = entry.getCurrency();
                if (currency == null || currency.isBlank() || "EGP".equalsIgnoreCase(currency)) continue;

                BigDecimal debit = line.getDebit() == null ? BigDecimal.ZERO : line.getDebit();
                BigDecimal credit = line.getCredit() == null ? BigDecimal.ZERO : line.getCredit();
                BigDecimal net = debit.subtract(credit);

                netBalanceByCurrency.merge(currency.toUpperCase(), net, BigDecimal::add);
            }
        }

        Map<String, Currency> currencyByCode = currencyRepository.findAllByOrderByCodeAsc().stream()
                .collect(Collectors.toMap(c -> c.getCode().toUpperCase(), c -> c));

        List<FxRevaluationApi.CurrencyResult> results = new ArrayList<>();
        int journalsPosted = 0;

        for (Map.Entry<String, BigDecimal> entry : netBalanceByCurrency.entrySet()) {
            String currencyCode = entry.getKey();
            BigDecimal netBalance = entry.getValue();

            if (netBalance.signum() == 0) continue;

            if (fxRevaluationPostRepository.existsByCurrencyCodeAndYearMonth(currencyCode, yearMonth)) {
                log.info("Skipping {} for {} — already posted", currencyCode, yearMonth);
                continue;
            }

            Currency currency = currencyByCode.get(currencyCode);
            if (currency == null) continue;

            BigDecimal currentRate = currency.getExchangeRate();
            if (currentRate == null || currentRate.signum() == 0) continue;

            BigDecimal bookValueInEgp = netBalance.multiply(currentRate).setScale(2, RoundingMode.HALF_UP);

            BigDecimal previousBookValue = fxRevaluationPostRepository
                    .findFirstByCurrencyCodeOrderByYearMonthDesc(currencyCode)
                    .map(p -> p.getTotalUnrealizedGain().subtract(p.getTotalUnrealizedLoss()))
                    .orElse(BigDecimal.ZERO);

            BigDecimal delta = bookValueInEgp.subtract(previousBookValue);
            if (delta.signum() == 0) {
                log.info("No FX movement for {} at {} — delta is zero, skipping", currencyCode, yearMonth);
                results.add(new FxRevaluationApi.CurrencyResult(
                        currencyCode, netBalance, currentRate, bookValueInEgp,
                        BigDecimal.ZERO, null));
                continue;
            }

            String entryNumber = "FX-REV-" + yearMonth + "-" + currencyCode;
            String description = "Month-end FX revaluation " + currencyCode + " " + yearMonth;
            long entryDateMs = asOf.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

            String gainCode = resolveCode(fxGainAccountCode, "4200");
            String lossCode = resolveCode(fxLossAccountCode, "4200");
            String holdingCode = resolveCode(fxHoldingAccountCode, "10101");
            Account fxGainAccount = findAccountByCodeOrNull(gainCode);
            Account fxLossAccount = findAccountByCodeOrNull(lossCode);
            Account currencyHoldingAccount = findAccountByCodeOrNull(holdingCode);

            if (fxGainAccount == null || fxLossAccount == null || currencyHoldingAccount == null) {
                log.warn("Skipping FX revaluation for {} — revaluation account config missing (gain={}, loss={}, holding={})",
                        currencyCode, gainCode, lossCode, holdingCode);
                results.add(new FxRevaluationApi.CurrencyResult(
                        currencyCode, netBalance, currentRate, bookValueInEgp,
                        delta, null, "FX_REVALUATION_ACCOUNT_NOT_FOUND"));
                continue;
            }

            BigDecimal absAmount = delta.abs();

            List<AccountingApi.JournalEntryLinePayload> lines = new ArrayList<>();
            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(new AccountingApi.JournalEntryLinePayload(
                        fxGainAccount.getId(), null, absAmount, null,
                        "FX unrealized gain " + currencyCode, null, null, null));
                lines.add(new AccountingApi.JournalEntryLinePayload(
                        currencyHoldingAccount.getId(), null, null, absAmount,
                        "FX unrealized gain " + currencyCode, null, null, null));
            } else {
                lines.add(new AccountingApi.JournalEntryLinePayload(
                        currencyHoldingAccount.getId(), null, absAmount, null,
                        "FX unrealized loss " + currencyCode, null, null, null));
                lines.add(new AccountingApi.JournalEntryLinePayload(
                        fxLossAccount.getId(), null, null, absAmount,
                        "FX unrealized loss " + currencyCode, null, null, null));
            }

            AccountingApi.JournalEntryPayload journalPayload = new AccountingApi.JournalEntryPayload(
                    entryNumber, entryDateMs, description, "FX_REVALUATION",
                    null, "EGP", null, null, null, lines);

            AccountingApi.JournalEntryResponse journalResponse =
                    journalEntryService.create(journalPayload, username);

            AccountingApi.JournalActionRequest postRequest = new AccountingApi.JournalActionRequest(
                    "FX-REV-" + yearMonth + "-" + currencyCode, null, null);
            journalEntryService.post(journalResponse.id(), postRequest, username);

            BigDecimal cumulativeGain = bookValueInEgp.compareTo(BigDecimal.ZERO) > 0
                    ? bookValueInEgp : BigDecimal.ZERO;
            BigDecimal cumulativeLoss = bookValueInEgp.compareTo(BigDecimal.ZERO) < 0
                    ? bookValueInEgp.abs() : BigDecimal.ZERO;

            FxRevaluationPost post = new FxRevaluationPost(
                    currencyCode, yearMonth, cumulativeGain, cumulativeLoss,
                    journalResponse.id(), username);
            fxRevaluationPostRepository.save(post);

            journalsPosted++;
            results.add(new FxRevaluationApi.CurrencyResult(
                    currencyCode, netBalance, currentRate, bookValueInEgp,
                    delta, journalResponse.id()));

            log.info("FX revaluation posted for {}: net={}, rate={}, egp={}, delta={}, journal={}",
                    currencyCode, netBalance, currentRate, bookValueInEgp, delta, journalResponse.id());
        }

        return new FxRevaluationApi.RevaluationRunResponse(
                results.size(), journalsPosted, results);
    }

    @Transactional(readOnly = true)
    public List<FxRevaluationApi.RevaluationResponse> getHistory() {
        return fxRevaluationPostRepository.findAllByOrderByPostedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Account findAccountByCodeOrNull(String code) {
        return accountRepository.findAllByOrderByCodeAsc().stream()
                .filter(a -> code.equals(a.getCode()))
                .findFirst()
                .orElse(null);
    }

    private String resolveCode(String configured, String fallback) {
        return (configured == null || configured.isBlank()) ? fallback : configured.trim();
    }

    private FxRevaluationApi.RevaluationResponse toResponse(FxRevaluationPost p) {
        return new FxRevaluationApi.RevaluationResponse(
                p.getId(), p.getCurrencyCode(), p.getYearMonth(),
                p.getTotalUnrealizedGain(), p.getTotalUnrealizedLoss(),
                p.getJournalEntryId(), p.getPostedBy(),
                p.getPostedAt().toEpochMilli(), p.getCreatedAt());
    }
}
