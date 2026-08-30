package com.bemo.hr.finance.infrastructure;

import com.bemo.hr.finance.domain.treasury.CommercialCheque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommercialChequeRepository extends JpaRepository<CommercialCheque, String> {
    List<CommercialCheque> findAllByOrderByDueDateAscCreatedAtDesc();
    List<CommercialCheque> findByChequeTypeOrderByDueDateAsc(CommercialCheque.ChequeType chequeType);
    List<CommercialCheque> findByStatusOrderByDueDateAsc(CommercialCheque.Status status);
    Optional<CommercialCheque> findByChequeNumber(String chequeNumber);
}
