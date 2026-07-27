package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    List<BankAccount> findAllByOrderByBankNameAsc();
}
