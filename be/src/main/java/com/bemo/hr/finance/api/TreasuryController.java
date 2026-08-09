package com.bemo.hr.finance.api;

import com.bemo.hr.finance.application.ExchangeRateHintService;
import com.bemo.hr.finance.domain.BankAccount;
import com.bemo.hr.finance.domain.Currency;
import com.bemo.hr.finance.domain.TaxRate;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CurrencyRepository;
import com.bemo.hr.finance.infrastructure.TaxRateRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
public class TreasuryController {

    private final BankAccountRepository bankAccountRepository;
    private final TaxRateRepository taxRateRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateHintService exchangeRateHintService;

    public TreasuryController(BankAccountRepository bankAccountRepository,
                              TaxRateRepository taxRateRepository,
                              CurrencyRepository currencyRepository,
                              ExchangeRateHintService exchangeRateHintService) {
        this.bankAccountRepository = bankAccountRepository;
        this.taxRateRepository = taxRateRepository;
        this.currencyRepository = currencyRepository;
        this.exchangeRateHintService = exchangeRateHintService;
    }

    // --- Bank Accounts ---
    @GetMapping("/banks")
    public List<TreasuryApi.BankAccountResponse> listBankAccounts() {
        return bankAccountRepository.findAllByOrderByBankNameAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/banks")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public TreasuryApi.BankAccountResponse createBankAccount(@Valid @RequestBody TreasuryApi.BankAccountPayload payload) {
        BankAccount bank = new BankAccount(payload.bankName(), payload.accountNumber(), payload.iban(), payload.swiftCode(), payload.accountId(), payload.active());
        return toResponse(bankAccountRepository.save(bank));
    }

    @PutMapping("/banks/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public TreasuryApi.BankAccountResponse updateBankAccount(@PathVariable String id, @Valid @RequestBody TreasuryApi.BankAccountPayload payload) {
        BankAccount bank = bankAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("الحساب البنكي غير موجود", "FIN_BANK_ACCOUNT_NOT_FOUND", HttpStatus.CONFLICT));
        bank.update(payload.bankName(), payload.accountNumber(), payload.iban(), payload.swiftCode(), payload.accountId(), payload.active());
        return toResponse(bankAccountRepository.save(bank));
    }

    // --- Taxes ---
    @GetMapping("/taxes")
    public List<TreasuryApi.TaxRateResponse> listTaxes() {
        return taxRateRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/taxes")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public TreasuryApi.TaxRateResponse createTaxRate(@Valid @RequestBody TreasuryApi.TaxRatePayload payload) {
        TaxRate.Type type = TaxRate.Type.valueOf(payload.taxType().toUpperCase());
        TaxRate tax = new TaxRate(payload.code(), payload.name(), payload.ratePercentage(), type, payload.accountId(), payload.active());
        return toResponse(taxRateRepository.save(tax));
    }

    @PutMapping("/taxes/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public TreasuryApi.TaxRateResponse updateTaxRate(@PathVariable String id, @Valid @RequestBody TreasuryApi.TaxRatePayload payload) {
        TaxRate tax = taxRateRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("ضريبة النظام غير موجودة", "FIN_SYSTEM_TAX_NOT_FOUND", HttpStatus.CONFLICT));
        TaxRate.Type type = TaxRate.Type.valueOf(payload.taxType().toUpperCase());
        tax.update(payload.code(), payload.name(), payload.ratePercentage(), type, payload.accountId(), payload.active());
        return toResponse(taxRateRepository.save(tax));
    }

    // --- Currencies ---
    @GetMapping("/currencies")
    public List<TreasuryApi.CurrencyResponse> listCurrencies() {
        return currencyRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/currencies")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public TreasuryApi.CurrencyResponse createCurrency(@Valid @RequestBody TreasuryApi.CurrencyPayload payload) {
        Currency currency = new Currency(payload.code(), payload.name(), payload.symbol(), payload.isBase(), payload.exchangeRate(), payload.active());
        return toResponse(currencyRepository.save(currency));
    }

    @PutMapping("/currencies/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER')")
    public TreasuryApi.CurrencyResponse updateCurrency(@PathVariable String id, @Valid @RequestBody TreasuryApi.CurrencyPayload payload) {
        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("العملة غير موجودة", "FIN_CURRENCY_NOT_FOUND", HttpStatus.CONFLICT));
        currency.update(payload.code(), payload.name(), payload.symbol(), payload.isBase(), payload.exchangeRate(), payload.active());
        return toResponse(currencyRepository.save(currency));
    }

    // --- Online exchange-rate hints (Frankfurter) ---
    @GetMapping("/exchange-rate-hints/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER', 'ACCOUNTANT', 'TREASURY_USER', 'AUDITOR')")
    public ExchangeRateHintApi.SettingsResponse exchangeRateHintSettings() {
        return exchangeRateHintService.settings();
    }

    @PutMapping("/exchange-rate-hints/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ExchangeRateHintApi.SettingsResponse updateExchangeRateHintSettings(
            @Valid @RequestBody ExchangeRateHintApi.SettingsRequest request) {
        return exchangeRateHintService.updateSettings(request);
    }

    @PostMapping("/exchange-rate-hints/refresh")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')")
    public ExchangeRateHintApi.RefreshResponse refreshExchangeRateHints() {
        return exchangeRateHintService.refreshNow();
    }

    private TreasuryApi.BankAccountResponse toResponse(BankAccount b) {
        return new TreasuryApi.BankAccountResponse(
                b.getId(), b.getBankName(), b.getAccountNumber(), b.getIban(), b.getSwiftCode(),
                b.getAccountId(), b.isActive(), b.getCreatedAt(), b.getUpdatedAt()
        );
    }

    private TreasuryApi.TaxRateResponse toResponse(TaxRate t) {
        return new TreasuryApi.TaxRateResponse(
                t.getId(), t.getCode(), t.getName(), t.getRatePercentage(), t.getTaxType().name(),
                t.getAccountId(), t.isActive(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }

    private TreasuryApi.CurrencyResponse toResponse(Currency c) {
        Long providerDate = c.getReferenceRateDate() == null
                ? null
                : c.getReferenceRateDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        return new TreasuryApi.CurrencyResponse(
                c.getId(), c.getCode(), c.getName(), c.getSymbol(), c.isBase(),
                c.getExchangeRate(),
                c.getReferenceExchangeRate(),
                c.getReferenceRateProvider(),
                c.getReferenceRateBaseCode(),
                providerDate,
                c.getReferenceRateFetchedAt(),
                c.getReferenceRateSupported(),
                c.isActive(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
