package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FiscalPeriod;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, String> {
    List<FiscalPeriod> findByFiscalYearOrderByPeriodNumberAsc(int fiscalYear);

    List<FiscalPeriod> findAllByOrderByFiscalYearDescPeriodNumberAsc();

    Optional<FiscalPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
            LocalDate startDate, LocalDate endDate, java.util.Collection<FiscalPeriod.Status> statuses);

    /**
     * 2026-09-07 remediation (Low Finding L-1): all fiscal periods that overlap a requested
     * calendar range (period.startDate &lt;= rangeEnd AND period.endDate &gt;= rangeStart) — used
     * to give Executive Analytics an honest, read-only signal about fiscal-calendar coverage/status
     * for the period it's summarizing, without adding any new locking/validation behavior.
     */
    List<FiscalPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate rangeEnd, LocalDate rangeStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FiscalPeriod p where p.id = :id")
    Optional<FiscalPeriod> findByIdForUpdate(@Param("id") String id);
}
