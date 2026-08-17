package com.bemo.hr.product.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ProductEventRepository extends JpaRepository<ProductEvent, String> {
    Optional<ProductEvent> findByOperationId(String operationId);

    @Modifying
    @Query("delete from ProductEvent e where e.appId=:appId and e.occurredAt<:cutoff")
    int deleteRawBefore(@Param("appId") String appId, @Param("cutoff") Instant cutoff);
}
