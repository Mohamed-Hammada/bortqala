package com.bemo.hr.shared.security.devicesigning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceSigningChallengeRepository extends JpaRepository<DeviceSigningChallenge, String> {
    Optional<DeviceSigningChallenge> findByIdAndUserId(String id, String userId);
}
