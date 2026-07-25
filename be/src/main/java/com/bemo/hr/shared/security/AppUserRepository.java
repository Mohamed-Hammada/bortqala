package com.bemo.hr.shared.security;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByAppIdAndUsernameIgnoreCase(String appId, String username);

    @EntityGraph(attributePaths = "roles")
    List<AppUser> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    List<AppUser> findAllByAppIdOrderByDisplayNameAsc(String appId);

    boolean existsByAppIdAndUsernameIgnoreCase(String appId, String username);
    boolean existsByAppIdAndUsernameIgnoreCaseAndIdNot(String appId, String username, String id);
}
