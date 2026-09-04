package com.bemo.hr.trade.pos.infrastructure;

import com.bemo.hr.trade.pos.domain.ThermalPrinter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThermalPrinterRepository extends JpaRepository<ThermalPrinter, String> {

    List<ThermalPrinter> findAllByOrderByCreatedAtDesc();

    List<ThermalPrinter> findByActiveTrue();

    Optional<ThermalPrinter> findFirstByTerminalIdAndActiveTrue(String terminalId);

    Optional<ThermalPrinter> findFirstByBranchIdAndActiveTrue(String branchId);

    Optional<ThermalPrinter> findFirstByIsDefaultTrueAndActiveTrue();
}
