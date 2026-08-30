package com.bemo.hr.fleet.infrastructure;

import com.bemo.hr.fleet.domain.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, String> {
    List<VehicleDocument> findByAppIdOrderByExpiryDateAsc(String appId);
    List<VehicleDocument> findByAppIdAndVehicleIdOrderByExpiryDateAsc(String appId, String vehicleId);
    Optional<VehicleDocument> findByAppIdAndId(String appId, String id);
}
