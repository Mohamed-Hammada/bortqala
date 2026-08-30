package com.bemo.hr.trade.sales.infrastructure;

import com.bemo.hr.trade.sales.domain.SalesTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, String> {
    boolean existsByScopeAndTargetRefIdAndPeriod(SalesTarget.Scope scope, String targetRefId, String period);

    @Query("SELECT t FROM SalesTarget t WHERE t.period = :period ORDER BY t.scope, t.targetRefId")
    List<SalesTarget> findByPeriod(@Param("period") String period);
}
