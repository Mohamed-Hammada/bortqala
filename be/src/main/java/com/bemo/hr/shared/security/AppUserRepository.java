package com.bemo.hr.shared.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByAppIdAndUsernameIgnoreCase(String appId, String username);

    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByAppIdAndId(String appId, String id);

    @EntityGraph(attributePaths = "roles")
    List<AppUser> findAllByAppIdOrderByDisplayNameAsc(String appId);

    @EntityGraph(attributePaths = "roles")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.appId = :appId order by u.displayName asc")
    List<AppUser> lockAllByAppIdOrderByDisplayNameAsc(@Param("appId") String appId);

    boolean existsByAppIdAndUsernameIgnoreCase(String appId, String username);
    boolean existsByAppIdAndUsernameIgnoreCaseAndIdNot(String appId, String username, String id);
}
