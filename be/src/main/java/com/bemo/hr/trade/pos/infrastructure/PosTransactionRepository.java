package com.bemo.hr.trade.pos.infrastructure;

import com.bemo.hr.trade.pos.domain.PosTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
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

    /**
     * 2026-09-07 remediation (Performance Review P-1, plus a correctness fix found while
     * addressing it): Executive Analytics used to call {@code findAll()} and sum
     * {@code totalAmount} in a Java stream with NO status filter at all — silently counting
     * VOIDED and REFUNDED transactions as real revenue. This pushes the date-range filter into SQL
     * AND restricts to COMPLETED, matching {@link #sumTodaySales}'s existing convention. Supported
     * by {@code idx_pos_transactions_app_created_at}.
     */
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PosTransaction t WHERE t.createdAt >= :startInclusive AND t.createdAt <= :endInclusive AND t.status = 'COMPLETED'")
    java.math.BigDecimal sumCompletedInRange(@Param("startInclusive") long startInclusive, @Param("endInclusive") long endInclusive);

    /**
     * 2026-09-07 remediation (branch-filtering hardening, docs/FINAL_REMEDIATION_VERIFICATION):
     * PosTransaction has no branchId of its own, but a real join through terminalId ->
     * PosTerminal.branchId exists. Used to give the Owner Cockpit real, branch-scoped POS revenue
     * instead of either ignoring the requested branchId (tenant-wide leakage) or fabricating a split.
     */
    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM PosTransaction t WHERE t.terminalId IN :terminalIds AND t.createdAt >= :startInclusive AND t.createdAt <= :endInclusive AND t.status = 'COMPLETED'")
    BigDecimal sumCompletedInRangeForTerminals(@Param("terminalIds") Collection<String> terminalIds,
                                               @Param("startInclusive") long startInclusive,
                                               @Param("endInclusive") long endInclusive);
}
