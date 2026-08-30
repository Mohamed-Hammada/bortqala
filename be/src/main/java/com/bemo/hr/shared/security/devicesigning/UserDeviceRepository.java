package com.bemo.hr.shared.security.devicesigning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {
    List<UserDevice> findByUserIdOrderByEnrolledAtDesc(String userId);
    Optional<UserDevice> findByUserIdAndDeviceIdentifier(String userId, String deviceIdentifier);
    Optional<UserDevice> findByIdAndUserId(String id, String userId);
    boolean existsByUserIdAndDeviceIdentifier(String userId, String deviceIdentifier);
}
