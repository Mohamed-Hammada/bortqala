package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.treasury.BankDifferencePosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankDifferencePostingRepository extends JpaRepository<BankDifferencePosting, String> {
    List<BankDifferencePosting> findByStatementLineId(String statementLineId);
}
