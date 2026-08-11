package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.ExchangeRateHintSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateHintSettingRepository extends JpaRepository<ExchangeRateHintSetting, String> {
    Optional<ExchangeRateHintSetting> findFirstByOrderByCreatedAtAsc();
}
