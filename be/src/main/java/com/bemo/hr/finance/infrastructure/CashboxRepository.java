package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.treasury.Cashbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashboxRepository extends JpaRepository<Cashbox, String> {
    List<Cashbox> findAllByOrderByCreatedAtDesc();
    List<Cashbox> findByActiveTrueOrderByCodeAsc();
    Optional<Cashbox> findByCode(String code);
    boolean existsByCode(String code);
}
