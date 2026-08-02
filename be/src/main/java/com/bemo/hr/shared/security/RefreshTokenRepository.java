package com.bemo.hr.shared.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByAppIdAndTokenHash(String appId, String tokenHash);
    Optional<RefreshToken> findTopByAppIdAndUserIdAndTokenHashAndRevokedAtIsNullOrderByCreatedAtDesc(String appId, String userId, String tokenHash);
    List<RefreshToken> findAllByAppIdAndUserIdAndRevokedAtIsNull(String appId, String userId);
    List<RefreshToken> findAllByAppIdAndRevokedAtIsNull(String appId);
}
