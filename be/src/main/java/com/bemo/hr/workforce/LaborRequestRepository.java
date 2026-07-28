package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LaborRequestRepository extends JpaRepository<LaborRequest, String> {
    Optional<LaborRequest> findByRequestNumber(String requestNumber);
    List<LaborRequest> findByContractorId(String contractorId);
    List<LaborRequest> findByStatus(String status);
}
