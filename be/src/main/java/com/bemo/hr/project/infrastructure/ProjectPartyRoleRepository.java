package com.bemo.hr.project.infrastructure;

import com.bemo.hr.project.domain.ProjectPartyRole;
import com.bemo.hr.project.domain.ProjectPartyRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPartyRoleRepository extends JpaRepository<ProjectPartyRole, String> {

    List<ProjectPartyRole> findByProjectId(String projectId);

    Optional<ProjectPartyRole> findByProjectIdAndPartyIdAndRoleType(String projectId, String partyId, ProjectPartyRoleType roleType);

    boolean existsByProjectIdAndPartyIdAndRoleType(String projectId, String partyId, ProjectPartyRoleType roleType);

    void deleteByProjectId(String projectId);
}
