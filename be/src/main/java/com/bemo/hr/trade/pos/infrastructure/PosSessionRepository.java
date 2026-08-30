package com.bemo.hr.trade.pos.infrastructure;

import com.bemo.hr.trade.pos.domain.PosSession;
import com.bemo.hr.trade.pos.domain.PosSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosSessionRepository extends JpaRepository<PosSession, String> {
    Optional<PosSession> findFirstByTerminalIdAndStatus(String terminalId, PosSessionStatus status);
    List<PosSession> findAllByTerminalIdOrderByOpenedAtDesc(String terminalId);
    List<PosSession> findAllByOrderByOpenedAtDesc();
    long countByStatus(PosSessionStatus status);
}
