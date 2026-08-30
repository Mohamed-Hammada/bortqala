package com.bemo.hr.compliance.eta.infrastructure;

import com.bemo.hr.compliance.eta.domain.EtaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtaConfigRepository extends JpaRepository<EtaConfig, String> {
    Optional<EtaConfig> findFirstByActiveTrue();
}
