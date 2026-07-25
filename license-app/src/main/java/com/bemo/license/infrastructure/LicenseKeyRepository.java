package com.bemo.license.infrastructure;
import com.bemo.license.domain.LicenseKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LicenseKeyRepository extends JpaRepository<LicenseKey,String> { Optional<LicenseKey> findByKeyHash(String keyHash); }
