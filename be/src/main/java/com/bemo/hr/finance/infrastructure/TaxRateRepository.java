package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, String> {
    List<TaxRate> findAllByOrderByCodeAsc();
}
