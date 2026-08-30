package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, String> {
    List<RentalItem> findByAppIdOrderByCreatedAtDesc(String appId);
    List<RentalItem> findByAppIdAndStatus(String appId, RentalItem.Status status);
    Optional<RentalItem> findByAppIdAndId(String appId, String id);
    Optional<RentalItem> findByAppIdAndCode(String appId, String code);
    long countByAppId(String appId);
    long countByAppIdAndStatus(String appId, RentalItem.Status status);
}
