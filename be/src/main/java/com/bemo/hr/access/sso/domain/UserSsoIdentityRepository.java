package com.bemo.hr.access.sso.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSsoIdentityRepository extends JpaRepository<UserSsoIdentity, String> {
    Optional<UserSsoIdentity> findByAppIdAndProviderAndSubject(String appId, String provider, String subject);
    List<UserSsoIdentity> findByAppIdAndUserId(String appId, String userId);
    boolean existsByAppIdAndProviderAndSubject(String appId, String provider, String subject);
}
