package com.bemo.hr.shared.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TenantApplicationRepository extends JpaRepository<TenantApplication, String> {
    Optional<TenantApplication> findByCodeIgnoreCaseAndActiveTrue(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from TenantApplication a where a.id=:id")
    Optional<TenantApplication> findByIdForUpdate(@Param("id") String id);
}
