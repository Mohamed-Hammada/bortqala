package com.bemo.license.infrastructure;
import com.bemo.license.domain.LicenseKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface LicenseKeyRepository extends JpaRepository<LicenseKey,String> {
    Optional<LicenseKey> findByKeyHash(String keyHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select license from LicenseKey license where license.keyHash = :keyHash")
    Optional<LicenseKey> findByKeyHashForUpdate(@Param("keyHash") String keyHash);
}
