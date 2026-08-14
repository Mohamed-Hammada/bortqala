package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, String> {
    List<FiscalPeriod> findByFiscalYearOrderByPeriodNumberAsc(int fiscalYear);
    List<FiscalPeriod> findAllByOrderByFiscalYearDescPeriodNumberAsc();
    Optional<FiscalPeriod> findByStartDateLessThanEqualAndEndDateGreaterThanEqualAndStatusIn(
            LocalDate startDate, LocalDate endDate, java.util.Collection<FiscalPeriod.Status> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FiscalPeriod p where p.id = :id")
    Optional<FiscalPeriod> findByIdForUpdate(@Param("id") String id);
}
