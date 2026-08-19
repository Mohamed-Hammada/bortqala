package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.treasury.CashboxTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashboxTransactionRepository extends JpaRepository<CashboxTransaction, String> {
    List<CashboxTransaction> findByCashboxIdOrderByTransactionDateDescCreatedAtDesc(String cashboxId);
    List<CashboxTransaction> findByCashboxId(String cashboxId);
}
