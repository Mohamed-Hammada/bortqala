package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LaborDispatchRepository extends JpaRepository<LaborDispatch, String> {
    List<LaborDispatch> findByRequestId(String requestId);
    List<LaborDispatch> findByContractorId(String contractorId);
}
