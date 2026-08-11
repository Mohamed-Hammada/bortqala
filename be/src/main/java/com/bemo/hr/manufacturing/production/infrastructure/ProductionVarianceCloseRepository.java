package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.ProductionVarianceClose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionVarianceCloseRepository extends JpaRepository<ProductionVarianceClose, String> {
    Optional<ProductionVarianceClose> findByWorkOrderId(String workOrderId);
}
