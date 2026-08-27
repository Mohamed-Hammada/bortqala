package com.bemo.hr.marketing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, String> {
    List<Campaign> findByAppIdOrderByCreatedAtDesc(String appId);
}
