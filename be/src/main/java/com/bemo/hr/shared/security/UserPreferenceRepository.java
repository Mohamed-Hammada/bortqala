package com.bemo.hr.shared.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, String> {
    Optional<UserPreference> findByUserId(String userId);
}
