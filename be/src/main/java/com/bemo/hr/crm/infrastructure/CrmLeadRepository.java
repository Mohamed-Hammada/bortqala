package com.bemo.hr.crm.infrastructure;

import com.bemo.hr.crm.domain.CrmLead;
import com.bemo.hr.crm.domain.CrmLeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrmLeadRepository extends JpaRepository<CrmLead, String> {

    List<CrmLead> findAllByOrderByCreatedAtDesc();

    List<CrmLead> findByStatusOrderByCreatedAtDesc(CrmLeadStatus status);

    Optional<CrmLead> findByPhone(String phone);

    Optional<CrmLead> findByLeadCode(String leadCode);

    long countByStatus(CrmLeadStatus status);

    @Query("SELECT COUNT(l) FROM CrmLead l WHERE l.status != 'WON' AND l.status != 'LOST'")
    long countActiveDeals();
}
