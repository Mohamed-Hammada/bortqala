package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.BankStatementLine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, String> {
    List<BankStatementLine> findByStatementIdOrderByLineNumberAsc(String statementId);
    long countByStatementIdAndStatusIn(String statementId, List<BankStatementLine.Status> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from BankStatementLine l where l.id = :id") Optional<BankStatementLine> findByIdForUpdate(@Param("id") String id);
}
