package com.bemo.hr.platform.deployment.infrastructure;

import com.bemo.hr.platform.deployment.domain.TenantLicenseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantLicenseCertificateRepository extends JpaRepository<TenantLicenseCertificate, String> {

    Optional<TenantLicenseCertificate> findFirstByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT c FROM TenantLicenseCertificate c ORDER BY c.createdAt DESC")
    List<TenantLicenseCertificate> findAllOrdered();

    Optional<TenantLicenseCertificate> findByLicenseKeyHash(String licenseKeyHash);
}
