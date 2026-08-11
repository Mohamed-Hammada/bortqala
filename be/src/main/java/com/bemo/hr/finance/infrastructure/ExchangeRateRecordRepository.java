package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.ExchangeRateRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRecordRepository extends JpaRepository<ExchangeRateRecord, String> {
    Optional<ExchangeRateRecord> findByFromCurrencyAndToCurrencyAndEffectiveDate(String fromCurrency, String toCurrency, LocalDate effectiveDate);
}
