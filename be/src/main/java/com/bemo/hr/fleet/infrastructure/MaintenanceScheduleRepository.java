package com.bemo.hr.fleet.infrastructure;

import com.bemo.hr.fleet.domain.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, String> {
    List<MaintenanceSchedule> findByAppIdAndActiveTrue(String appId);
    List<MaintenanceSchedule> findByAppIdAndVehicleId(String appId, String vehicleId);
    Optional<MaintenanceSchedule> findByAppIdAndId(String appId, String id);
}
