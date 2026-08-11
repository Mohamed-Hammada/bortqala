package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.WorkCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkCenterRepository extends JpaRepository<WorkCenter, String> {
    Optional<WorkCenter> findByCode(String code);
}
