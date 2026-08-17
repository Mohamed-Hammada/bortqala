package com.bemo.hr.payroll.infrastructure;

import com.bemo.hr.payroll.domain.PayrollRunHeader;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRunHeaderRepository extends JpaRepository<PayrollRunHeader, String> {
    List<PayrollRunHeader> findByPeriodId(String periodId);

    Optional<PayrollRunHeader> findFirstByPeriodIdOrderByCreatedAtDesc(String periodId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PayrollRunHeader r where r.id = :id")
    Optional<PayrollRunHeader> findByIdForUpdate(@Param("id") String id);
}
