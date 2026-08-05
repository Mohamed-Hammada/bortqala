package com.bemo.hr.shared.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByAppIdAndTokenHash(String appId, String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findForRotationByAppIdAndTokenHash(String appId, String tokenHash);

    List<RefreshToken> findAllByAppIdAndUserIdAndRevokedAtIsNull(String appId, String userId);
    List<RefreshToken> findAllByAppIdAndRevokedAtIsNull(String appId);
    List<RefreshToken> findAllByAppIdAndFamilyIdAndRevokedAtIsNull(String appId, String familyId);

    @Modifying
    @Query("delete from RefreshToken token where token.appId = :appId and token.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("appId") String appId, @Param("cutoff") Instant cutoff);
}
