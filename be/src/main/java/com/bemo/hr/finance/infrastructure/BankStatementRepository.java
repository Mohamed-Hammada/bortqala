package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.BankStatement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankStatementRepository extends JpaRepository<BankStatement, String> {
    List<BankStatement> findAllByOrderByPeriodEndDescImportedAtDesc();
    Optional<BankStatement> findByBankAccountIdAndFileHash(String bankAccountId, String fileHash);
    boolean existsByBankAccountIdAndStatementReference(String bankAccountId, String statementReference);
    Optional<BankStatement> findFirstByBankAccountIdOrderByPeriodEndDesc(String bankAccountId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BankStatement s where s.id = :id") Optional<BankStatement> findByIdForUpdate(@Param("id") String id);
}
