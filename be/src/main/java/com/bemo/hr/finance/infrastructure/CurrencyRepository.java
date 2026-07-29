package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
    List<Currency> findAllByOrderByCodeAsc();
    Optional<Currency> findByCodeIgnoreCaseAndActiveTrue(String code);
}
