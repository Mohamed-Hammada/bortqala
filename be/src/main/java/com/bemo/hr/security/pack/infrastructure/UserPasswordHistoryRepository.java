package com.bemo.hr.security.pack.infrastructure;

import com.bemo.hr.security.pack.domain.UserPasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, String> {
    List<UserPasswordHistory> findByAppIdAndUserIdOrderByCreatedAtDesc(String appId, String userId);
}
