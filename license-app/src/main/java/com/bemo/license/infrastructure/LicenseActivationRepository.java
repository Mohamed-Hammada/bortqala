package com.bemo.license.infrastructure;
import com.bemo.license.domain.LicenseActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LicenseActivationRepository extends JpaRepository<LicenseActivation,String> {
    long countByLicenseIdAndActiveTrue(String licenseId);
    Optional<LicenseActivation> findByLicenseIdAndInstallationIdAndActiveTrue(String licenseId,String installationId);
}
