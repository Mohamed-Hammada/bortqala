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
}
