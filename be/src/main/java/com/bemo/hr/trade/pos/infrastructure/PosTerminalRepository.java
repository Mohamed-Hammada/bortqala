package com.bemo.hr.trade.pos.infrastructure;

import com.bemo.hr.trade.pos.domain.PosTerminal;
import com.bemo.hr.trade.pos.domain.PosTerminalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, String> {
    Optional<PosTerminal> findByTerminalCode(String terminalCode);
    List<PosTerminal> findAllByStatus(PosTerminalStatus status);
    List<PosTerminal> findAllByOrderByTerminalCodeAsc();

    /**
     * 2026-09-07 remediation (branch-filtering hardening): PosTransaction has no branchId of its
     * own, but every transaction carries a real terminalId, and PosTerminal.branchId is real and
     * populated — this is the legitimate join path for branch-scoped POS revenue.
     */
    List<PosTerminal> findByBranchId(String branchId);
}
