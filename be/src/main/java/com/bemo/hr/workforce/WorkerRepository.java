package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, String> {
    List<Worker> findByIdIn(java.util.Collection<String> ids);
    Optional<Worker> findByCode(String code);
    List<Worker> findByCodeIn(java.util.Collection<String> codes);
    List<Worker> findByContractorId(String contractorId);
    List<Worker> findByCategoryId(String categoryId);
    List<Worker> findByStatus(String status);
}
