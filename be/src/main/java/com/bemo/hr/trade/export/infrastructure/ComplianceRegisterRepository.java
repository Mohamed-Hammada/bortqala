package com.bemo.hr.trade.export.infrastructure;

import com.bemo.hr.trade.export.domain.ComplianceRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComplianceRegisterRepository extends JpaRepository<ComplianceRegister, String> {

    List<ComplianceRegister> findByLotReferenceOrderByTreatmentDateDesc(String lotReference);

    List<ComplianceRegister> findAllByOrderByTreatmentDateDesc();

    List<ComplianceRegister> findByTreatmentDateBetweenOrderByTreatmentDateDesc(LocalDate from, LocalDate to);
}
