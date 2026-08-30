package com.bemo.hr.fleet.infrastructure;

import com.bemo.hr.fleet.domain.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, String> {
    List<MaintenanceRecord> findByAppIdOrderByPerformedDateDesc(String appId);
    List<MaintenanceRecord> findByAppIdAndVehicleIdOrderByPerformedDateDesc(String appId, String vehicleId);
    Optional<MaintenanceRecord> findByAppIdAndId(String appId, String id);
}
