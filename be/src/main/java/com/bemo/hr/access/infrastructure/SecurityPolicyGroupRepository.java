package com.bemo.hr.access.infrastructure;

import com.bemo.hr.access.domain.SecurityPolicyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityPolicyGroupRepository extends JpaRepository<SecurityPolicyGroup, String> {

    Optional<SecurityPolicyGroup> findByIdAndAppId(String id, String appId);

    Optional<SecurityPolicyGroup> findByAppIdAndGroupNameIgnoreCase(String appId, String groupName);

    List<SecurityPolicyGroup> findAllByAppIdOrderByGroupNameAsc(String appId);

    boolean existsByAppIdAndGroupNameIgnoreCase(String appId, String groupName);
}
