package com.bemo.hr.crm.infrastructure;

import com.bemo.hr.crm.domain.CrmMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmMessageRepository extends JpaRepository<CrmMessage, String> {

    List<CrmMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
