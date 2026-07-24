package com.bemo.hr.shared.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantApplicationRepository extends JpaRepository<TenantApplication, String> {
    Optional<TenantApplication> findByCodeIgnoreCaseAndActiveTrue(String code);
}
