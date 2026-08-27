package com.bemo.hr.trade.export.infrastructure;

import com.bemo.hr.trade.export.domain.ExportShipment;
import com.bemo.hr.trade.export.domain.ExportShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExportShipmentRepository extends JpaRepository<ExportShipment, String> {

    List<ExportShipment> findAllByOrderByCreatedAtDesc();

    Optional<ExportShipment> findByAppIdAndShipmentNumber(String appId, String shipmentNumber);

    List<ExportShipment> findByStatusIn(List<ExportShipmentStatus> statuses);

    List<ExportShipment> findByCustomerPartyIdOrderByCreatedAtDesc(String customerPartyId);
}
