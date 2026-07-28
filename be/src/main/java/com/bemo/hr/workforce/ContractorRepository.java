package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ContractorRepository extends JpaRepository<Contractor, String> {
    Optional<Contractor> findByCode(String code);
    List<Contractor> findByStatus(String status);
}
