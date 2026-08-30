package com.bemo.hr.shared.security.devicesigning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceSignatureLogRepository extends JpaRepository<DeviceSignatureLog, String> {
    List<DeviceSignatureLog> findByUserIdOrderByVerifiedAtDesc(String userId);
}
