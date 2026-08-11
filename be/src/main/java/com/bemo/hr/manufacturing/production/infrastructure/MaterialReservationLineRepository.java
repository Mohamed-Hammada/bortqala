package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.MaterialReservationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialReservationLineRepository extends JpaRepository<MaterialReservationLine, String> {
    List<MaterialReservationLine> findByReservationId(String reservationId);
}
