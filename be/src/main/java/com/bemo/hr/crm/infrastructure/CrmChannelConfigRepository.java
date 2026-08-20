package com.bemo.hr.crm.infrastructure;

import com.bemo.hr.crm.domain.CrmChannelConfig;
import com.bemo.hr.crm.domain.CrmChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrmChannelConfigRepository extends JpaRepository<CrmChannelConfig, String> {

    List<CrmChannelConfig> findAllByOrderByCreatedAtDesc();

    Optional<CrmChannelConfig> findFirstByChannelTypeAndActiveTrue(CrmChannelType channelType);
}
