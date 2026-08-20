package com.bemo.hr.access.infrastructure;

import com.bemo.hr.access.domain.PolicyGroupPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PolicyGroupPermissionRepository extends JpaRepository<PolicyGroupPermission, String> {

    List<PolicyGroupPermission> findByAppIdAndPolicyGroupId(String appId, String policyGroupId);

    List<PolicyGroupPermission> findByPolicyGroupId(String policyGroupId);

    List<PolicyGroupPermission> findByPolicyGroupIdIn(Collection<String> policyGroupIds);

    void deleteByAppIdAndPolicyGroupId(String appId, String policyGroupId);

    void deleteByPolicyGroupId(String policyGroupId);
}
