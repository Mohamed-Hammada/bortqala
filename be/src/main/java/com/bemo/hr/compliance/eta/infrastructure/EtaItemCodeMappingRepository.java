package com.bemo.hr.compliance.eta.infrastructure;

import com.bemo.hr.compliance.eta.domain.EtaItemCodeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtaItemCodeMappingRepository extends JpaRepository<EtaItemCodeMapping, String> {

    Optional<EtaItemCodeMapping> findByItemId(String itemId);

    Optional<EtaItemCodeMapping> findByItemCode(String itemCode);

    List<EtaItemCodeMapping> findByActiveTrueOrderByCreatedAtDesc();

    List<EtaItemCodeMapping> findAllByOrderByCreatedAtDesc();
}
