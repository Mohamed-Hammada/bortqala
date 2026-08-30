package com.bemo.hr.verticals.infrastructure;

import com.bemo.hr.verticals.domain.ThreePlContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThreePlContractRepository extends JpaRepository<ThreePlContract, String> {
    List<ThreePlContract> findAllByOrderByCreatedAtDesc();
    Optional<ThreePlContract> findByContractCode(String contractCode);
}
