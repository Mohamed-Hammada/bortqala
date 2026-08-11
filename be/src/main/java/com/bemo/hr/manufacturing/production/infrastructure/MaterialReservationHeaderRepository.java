package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.MaterialReservationHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialReservationHeaderRepository extends JpaRepository<MaterialReservationHeader, String> {
    Optional<MaterialReservationHeader> findByWorkOrderId(String workOrderId);
}
