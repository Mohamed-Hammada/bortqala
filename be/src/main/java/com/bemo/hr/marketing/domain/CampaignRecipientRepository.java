package com.bemo.hr.marketing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, String> {
    List<CampaignRecipient> findByCampaignIdOrderByCreatedAtAsc(String campaignId);
    List<CampaignRecipient> findByCampaignIdAndStatus(String campaignId, String status);
    long countByCampaignIdAndStatus(String campaignId, String status);
}
