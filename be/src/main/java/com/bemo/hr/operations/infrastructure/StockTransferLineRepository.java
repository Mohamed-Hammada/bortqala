package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.StockTransferLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferLineRepository extends JpaRepository<StockTransferLine, String> {
    List<StockTransferLine> findByTransferId(String transferId);
}
