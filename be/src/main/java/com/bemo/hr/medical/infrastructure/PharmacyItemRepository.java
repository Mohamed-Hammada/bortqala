package com.bemo.hr.medical.infrastructure;

import com.bemo.hr.medical.domain.PharmacyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyItemRepository extends JpaRepository<PharmacyItem, String> {

    Optional<PharmacyItem> findByAppIdAndId(String appId, String id);

    Optional<PharmacyItem> findByAppIdAndItemId(String appId, String itemId);

    List<PharmacyItem> findAllByAppIdOrderByTradeNameAsc(String appId);

    List<PharmacyItem> findAllByAppIdAndControlledTrueOrderByTradeNameAsc(String appId);
}
