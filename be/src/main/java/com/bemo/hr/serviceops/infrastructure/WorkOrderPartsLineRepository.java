package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.WorkOrderPartsLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderPartsLineRepository extends JpaRepository<WorkOrderPartsLine, String> {
    List<WorkOrderPartsLine> findByAppIdAndWorkOrderId(String appId, String workOrderId);
}
