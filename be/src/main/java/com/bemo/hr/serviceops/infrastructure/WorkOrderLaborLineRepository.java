package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.WorkOrderLaborLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderLaborLineRepository extends JpaRepository<WorkOrderLaborLine, String> {
    List<WorkOrderLaborLine> findByAppIdAndWorkOrderId(String appId, String workOrderId);
}
