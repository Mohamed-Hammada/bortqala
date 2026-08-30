package com.bemo.hr.fleet.infrastructure;

import com.bemo.hr.fleet.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    List<Vehicle> findByAppIdOrderByCreatedAtDesc(String appId);
    Optional<Vehicle> findByAppIdAndId(String appId, String id);
    Optional<Vehicle> findByAppIdAndPlateNumber(String appId, String plateNumber);
    List<Vehicle> findByAppIdAndStatus(String appId, Vehicle.Status status);
}

