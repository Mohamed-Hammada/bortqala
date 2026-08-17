package com.bemo.hr.shared.security;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TenantApplicationRepository extends JpaRepository<TenantApplication, String> {
    Optional<TenantApplication> findByCodeIgnoreCaseAndActiveTrue(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TenantApplication a where a.id=:id")
    Optional<TenantApplication> findByIdForUpdate(@Param("id") String id);
}
