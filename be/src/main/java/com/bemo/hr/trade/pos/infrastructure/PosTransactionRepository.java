package com.bemo.hr.trade.pos.infrastructure;

import com.bemo.hr.trade.pos.domain.PosTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PosTransactionRepository extends JpaRepository<PosTransaction, String> {
    Optional<PosTransaction> findByTransactionNumber(String transactionNumber);
    Optional<PosTransaction> findByClientOfflineId(String clientOfflineId);
    List<PosTransaction> findAllBySessionIdOrderByCreatedAtDesc(String sessionId);
    List<PosTransaction> findAllByTerminalIdOrderByCreatedAtDesc(String terminalId);
    List<PosTransaction> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PosTransaction t WHERE t.createdAt >= :startOfDay AND t.status = 'COMPLETED'")
    BigDecimal sumTodaySales(@Param("startOfDay") long startOfDay);

    @Query("SELECT COUNT(t) FROM PosTransaction t WHERE t.createdAt >= :startOfDay")
    long countTodayTransactions(@Param("startOfDay") long startOfDay);
}
