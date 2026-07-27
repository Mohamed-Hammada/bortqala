package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findAllByOrderByCodeAsc();
}
