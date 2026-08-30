package com.bemo.hr.security.pack.infrastructure;

import com.bemo.hr.security.pack.domain.RoleIpAllowlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleIpAllowlistRepository extends JpaRepository<RoleIpAllowlist, String> {
    List<RoleIpAllowlist> findByAppId(String appId);
    List<RoleIpAllowlist> findByAppIdAndRoleCode(String appId, String roleCode);
}
