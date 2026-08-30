package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, String> {
    List<WorkOrder> findByAppIdOrderByCreatedAtDesc(String appId);
    List<WorkOrder> findByAppIdAndStatus(String appId, WorkOrder.Status status);
    List<WorkOrder> findByAppIdAndAssignedEmployeeId(String appId, String assignedEmployeeId);
    Optional<WorkOrder> findByAppIdAndId(String appId, String id);
    Optional<WorkOrder> findByAppIdAndTicketNo(String appId, String ticketNo);
    long countByAppId(String appId);
    long countByAppIdAndStatus(String appId, WorkOrder.Status status);
}
