package com.bemo.hr.fleet.infrastructure;

import com.bemo.hr.fleet.domain.FuelLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelLogRepository extends JpaRepository<FuelLog, String> {
    List<FuelLog> findByAppIdOrderByOdometerAsc(String appId);
    List<FuelLog> findByAppIdAndVehicleIdOrderByOdometerAsc(String appId, String vehicleId);
    List<FuelLog> findByAppIdAndVehicleIdOrderByCreatedAtDesc(String appId, String vehicleId);
    Optional<FuelLog> findByAppIdAndId(String appId, String id);
    List<FuelLog> findByAppIdOrderByCreatedAtDesc(String appId);
}
