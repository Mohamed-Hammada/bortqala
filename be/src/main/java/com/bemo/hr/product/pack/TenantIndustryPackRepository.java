package com.bemo.hr.product.pack;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantIndustryPackRepository extends JpaRepository<TenantIndustryPack, String> {
    Optional<TenantIndustryPack> findByPackId(String packId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TenantIndustryPack p where p.packId=:packId")
    Optional<TenantIndustryPack> findByPackIdForUpdate(@Param("packId") String packId);

    Optional<TenantIndustryPack> findByOperationId(String operationId);

    List<TenantIndustryPack> findAll();
}
