package com.bemo.hr.fleet.application;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.fleet.api.FleetApi;
import com.bemo.hr.fleet.domain.*;
import com.bemo.hr.fleet.infrastructure.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FleetService {

    private static final Logger log = LoggerFactory.getLogger(FleetService.class);

    private final VehicleRepository vehicleRepository;
    private final FuelLogRepository fuelLogRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final VehicleDocumentRepository documentRepository;

    public FleetService(VehicleRepository vehicleRepository,
                        FuelLogRepository fuelLogRepository,
                        MaintenanceScheduleRepository scheduleRepository,
                        MaintenanceRecordRepository recordRepository,
                        VehicleDocumentRepository documentRepository) {
        this.vehicleRepository = vehicleRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.scheduleRepository = scheduleRepository;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
    }

    // ==========================================
    // 1. Vehicle Management
    // ==========================================

    @Transactional
    public FleetApi.VehicleResponse createVehicle(FleetApi.VehicleCreateRequest request) {
        String appId = TenantContext.require();

        vehicleRepository.findByAppIdAndPlateNumber(appId, request.plateNumber().trim())
                .ifPresent(v -> {
                    throw new BusinessRuleException("FLEET_PLATE_ALREADY_EXISTS", "FLEET_PLATE_ALREADY_EXISTS", HttpStatus.BAD_REQUEST);
                });

        Vehicle vehicle = new Vehicle(
                appId,
                request.plateNumber().trim(),
                request.make().trim(),
                request.model().trim(),
                request.year(),
                request.vehicleType(),
                request.assetId(),
                request.defaultDriverId(),
                request.defaultDriverName(),
                request.initialOdometer(),
                request.notes()
        );

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Created vehicle {} with plate {}", saved.getId(), saved.getPlateNumber());
        return toVehicleResponse(saved);
    }

    public List<FleetApi.VehicleResponse> listVehicles() {
        String appId = TenantContext.require();
        return vehicleRepository.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .map(this::toVehicleResponse)
                .collect(Collectors.toList());
    }

    public FleetApi.VehicleResponse getVehicle(String id) {
        String appId = TenantContext.require();
        Vehicle vehicle = vehicleRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("FLEET_VEHICLE_NOT_FOUND", "FLEET_VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toVehicleResponse(vehicle);
    }

    @Transactional
    public FleetApi.VehicleResponse updateVehicleStatus(String id, Vehicle.Status status) {
        String appId = TenantContext.require();
        Vehicle vehicle = vehicleRepository.findByAppIdAndId(appId, id)
                .orElseThrow(() -> new BusinessRuleException("FLEET_VEHICLE_NOT_FOUND", "FLEET_VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND));
        vehicle.setStatus(status);
        Vehicle saved = vehicleRepository.save(vehicle);
        return toVehicleResponse(saved);
    }

    // ==========================================
    // 2. Fuel Logs & Efficiency Tracking
    // ==========================================

    @Transactional
    public FleetApi.FuelLogResponse logFuel(FleetApi.FuelLogCreateRequest request) {
        String appId = TenantContext.require();

        Vehicle vehicle = vehicleRepository.findByAppIdAndId(appId, request.vehicleId())
                .orElseThrow(() -> new BusinessRuleException("FLEET_VEHICLE_NOT_FOUND", "FLEET_VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (request.liters() == null || request.liters().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("FLEET_INVALID_LITERS", "FLEET_INVALID_LITERS", HttpStatus.BAD_REQUEST);
        }

        FuelLog fuelLog = new FuelLog(
                appId,
                request.vehicleId(),
                request.logDate(),
                request.liters(),
                request.odometer(),
                request.totalCost(),
                request.stationName(),
                request.driverName(),
                request.notes()
        );

        FuelLog saved = fuelLogRepository.save(fuelLog);

        // Update vehicle odometer if new log odometer is greater
        if (request.odometer().compareTo(vehicle.getCurrentOdometer()) > 0) {
            vehicle.setCurrentOdometer(request.odometer());
            vehicleRepository.save(vehicle);
        }

        log.info("Recorded fuel log {} for vehicle {}", saved.getId(), vehicle.getPlateNumber());
        return toFuelLogResponse(saved, computeEfficiency(appId, saved));
    }

    public List<FleetApi.FuelLogResponse> listFuelLogs(String vehicleId) {
        String appId = TenantContext.require();
        List<FuelLog> logs = (vehicleId != null && !vehicleId.isBlank())
                ? fuelLogRepository.findByAppIdAndVehicleIdOrderByOdometerAsc(appId, vehicleId)
                : fuelLogRepository.findByAppIdOrderByOdometerAsc(appId);

        // Map consecutive efficiencies
        List<FleetApi.FuelLogResponse> responses = new ArrayList<>();
        FuelLog prev = null;
        for (FuelLog logItem : logs) {
            BigDecimal efficiency = null;
            if (prev != null && logItem.getOdometer() != null && prev.getOdometer() != null) {
                BigDecimal kmDiff = logItem.getOdometer().subtract(prev.getOdometer());
                if (kmDiff.compareTo(BigDecimal.ZERO) > 0 && logItem.getLiters().compareTo(BigDecimal.ZERO) > 0) {
                    efficiency = kmDiff.divide(logItem.getLiters(), 2, RoundingMode.HALF_UP);
                }
            }
            responses.add(toFuelLogResponse(logItem, efficiency));
            prev = logItem;
        }

        Collections.reverse(responses);
        return responses;
    }

    private BigDecimal computeEfficiency(String appId, FuelLog currentLog) {
        List<FuelLog> previousLogs = fuelLogRepository.findByAppIdAndVehicleIdOrderByOdometerAsc(appId, currentLog.getVehicleId());
        FuelLog prev = null;
        for (FuelLog l : previousLogs) {
            if (l.getId().equals(currentLog.getId())) {
                break;
            }
            prev = l;
        }
        if (prev != null && currentLog.getOdometer() != null && prev.getOdometer() != null) {
            BigDecimal kmDiff = currentLog.getOdometer().subtract(prev.getOdometer());
            if (kmDiff.compareTo(BigDecimal.ZERO) > 0 && currentLog.getLiters().compareTo(BigDecimal.ZERO) > 0) {
                return kmDiff.divide(currentLog.getLiters(), 2, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    // ==========================================
    // 3. Maintenance Schedules & Records
    // ==========================================

    @Transactional
    public FleetApi.MaintenanceScheduleResponse createSchedule(FleetApi.MaintenanceScheduleCreateRequest request) {
        String appId = TenantContext.require();

        Vehicle vehicle = vehicleRepository.findByAppIdAndId(appId, request.vehicleId())
                .orElseThrow(() -> new BusinessRuleException("FLEET_VEHICLE_NOT_FOUND", "FLEET_VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        MaintenanceSchedule schedule = new MaintenanceSchedule(
                appId,
                request.vehicleId(),
                request.title(),
                request.maintenanceKind(),
                request.intervalKm(),
                request.intervalDays(),
                request.lastDoneOdometer() != null ? request.lastDoneOdometer() : vehicle.getCurrentOdometer(),
                request.lastDoneDate() != null ? request.lastDoneDate() : LocalDate.now().toString()
        );

        MaintenanceSchedule saved = scheduleRepository.save(schedule);
        log.info("Created maintenance schedule {} for vehicle {}", saved.getId(), vehicle.getPlateNumber());
        return toScheduleResponse(saved, vehicle);
    }

    public List<FleetApi.MaintenanceScheduleResponse> listSchedules(String vehicleId) {
        String appId = TenantContext.require();
        List<MaintenanceSchedule> schedules = (vehicleId != null && !vehicleId.isBlank())
                ? scheduleRepository.findByAppIdAndVehicleId(appId, vehicleId)
                : scheduleRepository.findByAppIdAndActiveTrue(appId);

        Map<String, Vehicle> vehicleMap = vehicleRepository.findByAppIdOrderByCreatedAtDesc(appId).stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (a, b) -> a));

        return schedules.stream()
                .map(s -> toScheduleResponse(s, vehicleMap.get(s.getVehicleId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public FleetApi.MaintenanceRecordResponse recordMaintenance(FleetApi.MaintenanceRecordCreateRequest request) {
        String appId = TenantContext.require();

        Vehicle vehicle = vehicleRepository.findByAppIdAndId(appId, request.vehicleId())
                .orElseThrow(() -> new BusinessRuleException("FLEET_VEHICLE_NOT_FOUND", "FLEET_VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        MaintenanceRecord record = new MaintenanceRecord(
                appId,
                request.vehicleId(),
                request.scheduleId(),
                request.title(),
                request.performedDate(),
                request.odometer(),
                request.cost(),
                request.vendorPartyId(),
                request.vendorName(),
                request.description()
        );

        MaintenanceRecord saved = recordRepository.save(record);

        // If linked to a schedule, reset baseline
        if (request.scheduleId() != null && !request.scheduleId().isBlank()) {
            scheduleRepository.findByAppIdAndId(appId, request.scheduleId()).ifPresent(schedule -> {
                schedule.setLastDoneOdometer(request.odometer());
                schedule.setLastDoneDate(request.performedDate());
                scheduleRepository.save(schedule);
            });
        }

        // Update vehicle odometer if higher
        if (request.odometer().compareTo(vehicle.getCurrentOdometer()) > 0) {
            vehicle.setCurrentOdometer(request.odometer());
            vehicleRepository.save(vehicle);
        }

        log.info("Recorded maintenance {} for vehicle {}", saved.getId(), vehicle.getPlateNumber());
        return toRecordResponse(saved);
    }

    public List<FleetApi.MaintenanceRecordResponse> listRecords(String vehicleId) {
        String appId = TenantContext.require();
        List<MaintenanceRecord> records = (vehicleId != null && !vehicleId.isBlank())
                ? recordRepository.findByAppIdAndVehicleIdOrderByPerformedDateDesc(appId, vehicleId)
                : recordRepository.findByAppIdOrderByPerformedDateDesc(appId);

        return records.stream().map(this::toRecordResponse).collect(Collectors.toList());
    }

    // ==========================================
    // 4. Vehicle Documents & Renewals
    // ==========================================

    @Transactional
    public FleetApi.VehicleDocumentResponse addDocument(FleetApi.VehicleDocumentCreateRequest request) {
        String appId = TenantContext.require();

        Vehicle vehicle = vehicleRepository.findByAppIdAndId(appId, request.vehicleId())
                .orElseThrow(() -> new BusinessRuleException("FLEET_VEHICLE_NOT_FOUND", "FLEET_VEHICLE_NOT_FOUND", HttpStatus.NOT_FOUND));

        VehicleDocument doc = new VehicleDocument(
                appId,
                request.vehicleId(),
                request.documentType(),
                request.documentNumber(),
                request.issueDate(),
                request.expiryDate(),
                request.issuer(),
                request.notes()
        );

        VehicleDocument saved = documentRepository.save(doc);
        log.info("Added vehicle document {} for vehicle {}", saved.getId(), vehicle.getPlateNumber());
        return toDocumentResponse(saved);
    }

    public List<FleetApi.VehicleDocumentResponse> listDocuments(String vehicleId) {
        String appId = TenantContext.require();
        List<VehicleDocument> docs = (vehicleId != null && !vehicleId.isBlank())
                ? documentRepository.findByAppIdAndVehicleIdOrderByExpiryDateAsc(appId, vehicleId)
                : documentRepository.findByAppIdOrderByExpiryDateAsc(appId);

        return docs.stream().map(this::toDocumentResponse).collect(Collectors.toList());
    }

    // ==========================================
    // 5. Fleet Cost Report
    // ==========================================

    public FleetApi.FleetCostSummary getCostSummary() {
        String appId = TenantContext.require();

        List<Vehicle> vehicles = vehicleRepository.findByAppIdOrderByCreatedAtDesc(appId);
        List<FuelLog> fuelLogs = fuelLogRepository.findByAppIdOrderByCreatedAtDesc(appId);
        List<MaintenanceRecord> records = recordRepository.findByAppIdOrderByPerformedDateDesc(appId);

        BigDecimal totalFuelCost = fuelLogs.stream()
                .map(FuelLog::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMaintCost = records.stream()
                .map(MaintenanceRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = totalFuelCost.add(totalMaintCost);

        BigDecimal totalKm = vehicles.stream()
                .map(Vehicle::getCurrentOdometer)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costPerKm = BigDecimal.ZERO;
        if (totalKm.compareTo(BigDecimal.ZERO) > 0) {
            costPerKm = grandTotal.divide(totalKm, 2, RoundingMode.HALF_UP);
        }

        return new FleetApi.FleetCostSummary(
                vehicles.size(),
                totalFuelCost,
                totalMaintCost,
                grandTotal,
                totalKm,
                costPerKm
        );
    }

    // --- Helpers & Mappers ---

    private FleetApi.VehicleResponse toVehicleResponse(Vehicle v) {
        return new FleetApi.VehicleResponse(
                v.getId(),
                v.getPlateNumber(),
                v.getMake(),
                v.getModel(),
                v.getYear(),
                v.getVehicleType(),
                v.getAssetId(),
                null, // asset NBV integrated via WP-04
                v.getDefaultDriverId(),
                v.getDefaultDriverName(),
                v.getCurrentOdometer(),
                v.getStatus(),
                v.getNotes(),
                v.getCreatedAt(),
                v.getUpdatedAt()
        );
    }

    private FleetApi.FuelLogResponse toFuelLogResponse(FuelLog logItem, BigDecimal efficiency) {
        return new FleetApi.FuelLogResponse(
                logItem.getId(),
                logItem.getVehicleId(),
                logItem.getLogDate(),
                logItem.getLiters(),
                logItem.getOdometer(),
                logItem.getTotalCost(),
                efficiency,
                logItem.getStationName(),
                logItem.getDriverName(),
                logItem.getNotes(),
                logItem.getCreatedAt()
        );
    }

    private FleetApi.MaintenanceScheduleResponse toScheduleResponse(MaintenanceSchedule s, Vehicle vehicle) {
        boolean isDue = false;
        String dueReason = null;

        if (vehicle != null && s.isActive()) {
            // Check KM due
            if (s.getIntervalKm() != null && s.getIntervalKm().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lastOdo = s.getLastDoneOdometer() != null ? s.getLastDoneOdometer() : BigDecimal.ZERO;
                BigDecimal nextDueOdo = lastOdo.add(s.getIntervalKm());
                if (vehicle.getCurrentOdometer() != null && vehicle.getCurrentOdometer().compareTo(nextDueOdo) >= 0) {
                    isDue = true;
                    dueReason = "KM_INTERVAL_REACHED";
                }
            }

            // Check Days due
            if (!isDue && s.getIntervalDays() != null && s.getIntervalDays() > 0 && s.getLastDoneDate() != null) {
                try {
                    LocalDate lastDate = LocalDate.parse(s.getLastDoneDate());
                    long daysPassed = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
                    if (daysPassed >= s.getIntervalDays()) {
                        isDue = true;
                        dueReason = "DAYS_INTERVAL_REACHED";
                    }
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        return new FleetApi.MaintenanceScheduleResponse(
                s.getId(),
                s.getVehicleId(),
                s.getTitle(),
                s.getMaintenanceKind(),
                s.getIntervalKm(),
                s.getIntervalDays(),
                s.getLastDoneOdometer(),
                s.getLastDoneDate(),
                isDue,
                dueReason,
                s.isActive(),
                s.getCreatedAt()
        );
    }

    private FleetApi.MaintenanceRecordResponse toRecordResponse(MaintenanceRecord r) {
        return new FleetApi.MaintenanceRecordResponse(
                r.getId(),
                r.getVehicleId(),
                r.getScheduleId(),
                r.getTitle(),
                r.getPerformedDate(),
                r.getOdometer(),
                r.getCost(),
                r.getVendorPartyId(),
                r.getVendorName(),
                r.getDescription(),
                r.getCreatedAt()
        );
    }

    private FleetApi.VehicleDocumentResponse toDocumentResponse(VehicleDocument d) {
        boolean isExpired = false;
        boolean isDueSoon = false;

        if (d.getExpiryDate() != null && !d.getExpiryDate().isBlank()) {
            try {
                LocalDate exp = LocalDate.parse(d.getExpiryDate());
                LocalDate now = LocalDate.now();
                if (exp.isBefore(now)) {
                    isExpired = true;
                } else if (!exp.isAfter(now.plusDays(30))) {
                    isDueSoon = true;
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        return new FleetApi.VehicleDocumentResponse(
                d.getId(),
                d.getVehicleId(),
                d.getDocumentType(),
                d.getDocumentNumber(),
                d.getIssueDate(),
                d.getExpiryDate(),
                d.getIssuer(),
                isExpired,
                isDueSoon,
                d.getNotes(),
                d.getCreatedAt()
        );
    }
}
