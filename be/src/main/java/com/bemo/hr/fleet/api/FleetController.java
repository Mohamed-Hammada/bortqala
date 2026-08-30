package com.bemo.hr.fleet.api;

import com.bemo.hr.fleet.application.FleetService;
import com.bemo.hr.fleet.domain.Vehicle;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fleet")
public class FleetController {

    private final FleetService fleetService;

    public FleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    // --- Vehicles ---

    @PostMapping("/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<FleetApi.VehicleResponse> createVehicle(@Valid @RequestBody FleetApi.VehicleCreateRequest request) {
        FleetApi.VehicleResponse response = fleetService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/vehicles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FleetApi.VehicleResponse>> listVehicles() {
        return ResponseEntity.ok(fleetService.listVehicles());
    }

    @GetMapping("/vehicles/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FleetApi.VehicleResponse> getVehicle(@PathVariable String id) {
        return ResponseEntity.ok(fleetService.getVehicle(id));
    }

    @PutMapping("/vehicles/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<FleetApi.VehicleResponse> updateVehicleStatus(@PathVariable String id, @RequestParam Vehicle.Status status) {
        return ResponseEntity.ok(fleetService.updateVehicleStatus(id, status));
    }

    // --- Fuel Logs ---

    @PostMapping("/fuel-logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FleetApi.FuelLogResponse> logFuel(@Valid @RequestBody FleetApi.FuelLogCreateRequest request) {
        FleetApi.FuelLogResponse response = fleetService.logFuel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/fuel-logs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FleetApi.FuelLogResponse>> listFuelLogs(@RequestParam(required = false) String vehicleId) {
        return ResponseEntity.ok(fleetService.listFuelLogs(vehicleId));
    }

    // --- Maintenance Schedules ---

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<FleetApi.MaintenanceScheduleResponse> createSchedule(@Valid @RequestBody FleetApi.MaintenanceScheduleCreateRequest request) {
        FleetApi.MaintenanceScheduleResponse response = fleetService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/schedules")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FleetApi.MaintenanceScheduleResponse>> listSchedules(@RequestParam(required = false) String vehicleId) {
        return ResponseEntity.ok(fleetService.listSchedules(vehicleId));
    }

    // --- Maintenance Records ---

    @PostMapping("/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FleetApi.MaintenanceRecordResponse> recordMaintenance(@Valid @RequestBody FleetApi.MaintenanceRecordCreateRequest request) {
        FleetApi.MaintenanceRecordResponse response = fleetService.recordMaintenance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FleetApi.MaintenanceRecordResponse>> listRecords(@RequestParam(required = false) String vehicleId) {
        return ResponseEntity.ok(fleetService.listRecords(vehicleId));
    }

    // --- Vehicle Documents ---

    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'HR_MANAGER', 'INVENTORY_MANAGER')")
    public ResponseEntity<FleetApi.VehicleDocumentResponse> addDocument(@Valid @RequestBody FleetApi.VehicleDocumentCreateRequest request) {
        FleetApi.VehicleDocumentResponse response = fleetService.addDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FleetApi.VehicleDocumentResponse>> listDocuments(@RequestParam(required = false) String vehicleId) {
        return ResponseEntity.ok(fleetService.listDocuments(vehicleId));
    }

    // --- Cost Summary ---

    @GetMapping("/cost-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FleetApi.FleetCostSummary> getCostSummary() {
        return ResponseEntity.ok(fleetService.getCostSummary());
    }
}
