package com.bemo.hr.trade.export.infrastructure;

import com.bemo.hr.trade.export.domain.PesticideRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PesticideRegisterRepository extends JpaRepository<PesticideRegister, String> {

    List<PesticideRegister> findAllByOrderByChemicalNameAsc();

    Optional<PesticideRegister> findByChemicalNameIgnoreCase(String chemicalName);

    boolean existsByChemicalNameIgnoreCase(String chemicalName);
}
