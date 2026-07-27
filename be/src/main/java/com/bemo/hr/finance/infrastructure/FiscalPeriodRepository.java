package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, String> {
    List<FiscalPeriod> findByFiscalYearOrderByPeriodNumberAsc(int fiscalYear);
    List<FiscalPeriod> findAllByOrderByFiscalYearDescPeriodNumberAsc();
}
