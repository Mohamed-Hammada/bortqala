package com.bemo.hr.crm.infrastructure;

import com.bemo.hr.crm.domain.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmActivityRepository extends JpaRepository<CrmActivity, String> {

    List<CrmActivity> findByLeadIdOrderByCreatedAtDesc(String leadId);
}
