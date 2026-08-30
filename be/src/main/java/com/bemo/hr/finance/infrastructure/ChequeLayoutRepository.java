package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.treasury.ChequeLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeLayoutRepository extends JpaRepository<ChequeLayout, String> {
    List<ChequeLayout> findAllByActiveTrueOrderByBankCode();
    Optional<ChequeLayout> findByBankCode(String bankCode);
    boolean existsByBankCode(String bankCode);
}
