package com.bemo.hr.party;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierBankAccountRepository extends JpaRepository<SupplierBankAccount, String> {
    List<SupplierBankAccount> findBySupplierIdOrderByPrimaryDescCreatedAtAsc(String supplierId);

    Optional<SupplierBankAccount> findByNormalizedIban(String normalizedIban);

    boolean existsByNormalizedIbanAndSupplierIdNot(String normalizedIban, String supplierId);
}
