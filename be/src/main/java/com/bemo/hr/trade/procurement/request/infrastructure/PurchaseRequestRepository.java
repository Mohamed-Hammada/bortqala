package com.bemo.hr.trade.procurement.request.infrastructure;

import com.bemo.hr.trade.procurement.request.domain.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, String> {

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    List<PurchaseRequest> findByStatusOrderByCreatedAtDesc(PurchaseRequest.Status status);

    List<PurchaseRequest> findByDepartmentIdOrderByCreatedAtDesc(String departmentId);

    List<PurchaseRequest> findByStatusAndDepartmentIdOrderByCreatedAtDesc(PurchaseRequest.Status status, String departmentId);
}
