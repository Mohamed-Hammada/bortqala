package com.bemo.hr.security.pack.infrastructure;

import com.bemo.hr.security.pack.domain.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, String> {
    List<TrustedDevice> findByAppIdAndUserIdOrderByLastSeenAtDesc(String appId, String userId);
    Optional<TrustedDevice> findByAppIdAndUserIdAndDeviceId(String appId, String userId, String deviceId);
}
