package com.bemo.hr.trade.pos.infrastructure;

import com.bemo.hr.trade.pos.domain.PosTransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PosTransactionLineRepository extends JpaRepository<PosTransactionLine, String> {
    List<PosTransactionLine> findAllByTransactionId(String transactionId);
}
