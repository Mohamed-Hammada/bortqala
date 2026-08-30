package com.bemo.hr.security.pack.infrastructure;

import com.bemo.hr.security.pack.domain.UserTotp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTotpRepository extends JpaRepository<UserTotp, String> {
    Optional<UserTotp> findByAppIdAndUserId(String appId, String userId);
}
