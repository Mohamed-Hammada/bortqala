package com.bemo.hr.organization.infrastructure;

import com.bemo.hr.organization.domain.IntercompanyStatus;
import com.bemo.hr.organization.domain.IntercompanyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntercompanyTransactionRepository extends JpaRepository<IntercompanyTransaction, String> {

    List<IntercompanyTransaction> findAllByOrderByCreatedAtDesc();

    List<IntercompanyTransaction> findByStatus(IntercompanyStatus status);

    List<IntercompanyTransaction> findByEliminatedInPeriod(String eliminatedInPeriod);

    @Query("SELECT t FROM IntercompanyTransaction t WHERE t.transactionNumber LIKE CONCAT(:prefix, '%') ORDER BY t.transactionNumber DESC")
    List<IntercompanyTransaction> findLatestByNumberPrefix(@Param("prefix") String prefix);
}
