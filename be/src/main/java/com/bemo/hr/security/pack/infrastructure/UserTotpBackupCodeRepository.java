package com.bemo.hr.security.pack.infrastructure;

import com.bemo.hr.security.pack.domain.UserTotpBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTotpBackupCodeRepository extends JpaRepository<UserTotpBackupCode, String> {
    List<UserTotpBackupCode> findByAppIdAndUserIdAndUsedFalse(String appId, String userId);
    List<UserTotpBackupCode> findByAppIdAndUserId(String appId, String userId);
    void deleteByAppIdAndUserId(String appId, String userId);
}
