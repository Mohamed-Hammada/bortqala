package com.bemo.hr.crm.infrastructure;

import com.bemo.hr.crm.domain.CrmChannelType;
import com.bemo.hr.crm.domain.CrmConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrmConversationRepository extends JpaRepository<CrmConversation, String> {

    List<CrmConversation> findAllByOrderByUpdatedAtDesc();

    Optional<CrmConversation> findFirstByChannelTypeAndExternalSenderId(CrmChannelType channelType, String externalSenderId);

    @Query("SELECT COALESCE(SUM(c.unreadCount), 0) FROM CrmConversation c")
    long sumUnreadCount();
}
