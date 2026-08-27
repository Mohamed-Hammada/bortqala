package com.bemo.hr.compliance.einvoicing.infrastructure;

import com.bemo.hr.compliance.einvoicing.domain.EinvoicingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EinvoicingSettingsRepository extends JpaRepository<EinvoicingSettings, String> {
    Optional<EinvoicingSettings> findFirstByAppId(String appId);
    Optional<EinvoicingSettings> findFirstByAppIdOrderByUpdatedAtDesc(String appId);
}
