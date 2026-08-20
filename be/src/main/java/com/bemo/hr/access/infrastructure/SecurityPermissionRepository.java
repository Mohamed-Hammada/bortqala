package com.bemo.hr.access.infrastructure;

import com.bemo.hr.access.domain.SecurityPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SecurityPermissionRepository extends JpaRepository<SecurityPermission, String> {

    Optional<SecurityPermission> findByPermissionKey(String permissionKey);

    List<SecurityPermission> findAllByOrderByModuleAscSubmoduleAscActionAsc();

    List<SecurityPermission> findByIdIn(Collection<String> ids);

    List<SecurityPermission> findByPermissionKeyIn(Collection<String> permissionKeys);

    List<SecurityPermission> findByModuleOrderByPermissionKeyAsc(String module);
}
