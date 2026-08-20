package com.bemo.hr.access.infrastructure;

import com.bemo.hr.access.domain.UserPolicyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPolicyAssignmentRepository extends JpaRepository<UserPolicyAssignment, String> {

    List<UserPolicyAssignment> findByAppIdAndUserId(String appId, String userId);

    List<UserPolicyAssignment> findByUserId(String userId);

    void deleteByAppIdAndUserId(String appId, String userId);

    void deleteByUserId(String userId);

    void deleteByAppIdAndPolicyGroupId(String appId, String policyGroupId);

    long countByAppIdAndPolicyGroupId(String appId, String policyGroupId);
}
