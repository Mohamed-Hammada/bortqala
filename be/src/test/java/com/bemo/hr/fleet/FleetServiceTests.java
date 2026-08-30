package com.bemo.hr.fleet;

import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.fleet.api.FleetApi;
import com.bemo.hr.fleet.application.FleetService;
import com.bemo.hr.fleet.domain.*;
import com.bemo.hr.fleet.infrastructure.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FleetServiceTests {

    private static final String APP_ID = "app-test";

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private FuelLogRepository fuelLogRepository;

    @Mock
    private MaintenanceScheduleRepository scheduleRepository;

    @Mock
    private MaintenanceRecordRepository recordRepository;

    @Mock
    private VehicleDocumentRepository documentRepository;

    @InjectMocks
    private FleetService fleetService;

    @BeforeEach
    void setUp() {
        TenantContext.set(APP_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createVehicle_success() {
        FleetApi.VehicleCreateRequest req = new FleetApi.VehicleCreateRequest(
                "ABC-1234",
                "Toyota",
                "Hilux",
                2024,
                Vehicle.VehicleType.TRUCK,
                null,
                "emp-1",
                "Ahmed Driver",
                new BigDecimal("10000"),
                "Fleet van 1"
        );

        when(vehicleRepository.findByAppIdAndPlateNumber(APP_ID, "ABC-1234")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        FleetApi.VehicleResponse res = fleetService.createVehicle(req);
        assertThat(res.plateNumber()).isEqualTo("ABC-1234");
        assertThat(res.make()).isEqualTo("Toyota");
        assertThat(res.currentOdometer()).isEqualByComparingTo("10000");

    }

    @Test
    void createVehicle_duplicatePlate_throwsException() {
        FleetApi.VehicleCreateRequest req = new FleetApi.VehicleCreateRequest(
                "ABC-1234", "Toyota", "Hilux", 2024, Vehicle.VehicleType.TRUCK,
                null, null, null, BigDecimal.ZERO, null
        );

        Vehicle existing = new Vehicle(APP_ID, "ABC-1234", "Toyota", "Hilux", 2024, Vehicle.VehicleType.TRUCK, null, null, null, BigDecimal.ZERO, null);
        when(vehicleRepository.findByAppIdAndPlateNumber(APP_ID, "ABC-1234")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> fleetService.createVehicle(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("FLEET_PLATE_ALREADY_EXISTS");
    }

    @Test
    void logFuel_calculatesEfficiencyConsecutively_AC1() {
        Vehicle vehicle = new Vehicle(APP_ID, "ABC-1234", "Toyota", "Hilux", 2024, Vehicle.VehicleType.TRUCK, null, null, null, new BigDecimal("10000"), null);
        when(vehicleRepository.findByAppIdAndId(APP_ID, vehicle.getId())).thenReturn(Optional.of(vehicle));

        FuelLog prevLog = new FuelLog(APP_ID, vehicle.getId(), "2026-08-01", new BigDecimal("50"), new BigDecimal("10000"), new BigDecimal("500"), "Station A", "Driver", null);
        FuelLog currentLog = new FuelLog(APP_ID, vehicle.getId(), "2026-08-10", new BigDecimal("10"), new BigDecimal("10100"), new BigDecimal("150"), "Station B", "Driver", null);

        when(fuelLogRepository.save(any(FuelLog.class))).thenReturn(currentLog);
        when(fuelLogRepository.findByAppIdAndVehicleIdOrderByOdometerAsc(APP_ID, vehicle.getId()))
                .thenReturn(List.of(prevLog, currentLog));

        FleetApi.FuelLogCreateRequest req = new FleetApi.FuelLogCreateRequest(
                vehicle.getId(),
                "2026-08-10",
                new BigDecimal("10"), // 10 Liters
                new BigDecimal("10100"), // 100 km diff
                new BigDecimal("150"),
                "Station B",
                "Driver",
                null
        );

        FleetApi.FuelLogResponse res = fleetService.logFuel(req);
        assertThat(res.efficiencyKmPerLiter()).isNotNull();
        // 100 km / 10 L = 10.00 km/L
        assertThat(res.efficiencyKmPerLiter()).isEqualByComparingTo("10.00");
    }

    @Test
    void maintenanceSchedule_dueDetectionAndResetBaseline_AC2() {
        Vehicle vehicle = new Vehicle(APP_ID, "ABC-1234", "Toyota", "Hilux", 2024, Vehicle.VehicleType.TRUCK, null, null, null, new BigDecimal("15000"), null);
        when(vehicleRepository.findByAppIdOrderByCreatedAtDesc(APP_ID)).thenReturn(List.of(vehicle));

        // Schedule: every 5000 km, last done at 9000 km -> next due at 14000 km -> vehicle current is 15000 km -> DUE!
        MaintenanceSchedule schedule = new MaintenanceSchedule(
                APP_ID,
                vehicle.getId(),
                "Oil Change",
                MaintenanceSchedule.MaintenanceKind.OIL,
                new BigDecimal("5000"),
                null,
                new BigDecimal("9000"),
                "2026-06-01"
        );

        when(scheduleRepository.findByAppIdAndActiveTrue(APP_ID)).thenReturn(List.of(schedule));

        List<FleetApi.MaintenanceScheduleResponse> list = fleetService.listSchedules(null);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).isDue()).isTrue();
        assertThat(list.get(0).dueReason()).isEqualTo("KM_INTERVAL_REACHED");

        // Record maintenance completion -> resets lastDoneOdometer
        when(vehicleRepository.findByAppIdAndId(APP_ID, vehicle.getId())).thenReturn(Optional.of(vehicle));
        when(scheduleRepository.findByAppIdAndId(APP_ID, schedule.getId())).thenReturn(Optional.of(schedule));
        when(recordRepository.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        FleetApi.MaintenanceRecordCreateRequest recordReq = new FleetApi.MaintenanceRecordCreateRequest(
                vehicle.getId(),
                schedule.getId(),
                "Completed Oil Change",
                "2026-08-30",
                new BigDecimal("15000"),
                new BigDecimal("300"),
                null,
                "Workshop X",
                "Routine oil change"
        );

        FleetApi.MaintenanceRecordResponse recordRes = fleetService.recordMaintenance(recordReq);
        assertThat(recordRes.cost()).isEqualByComparingTo("300");
        assertThat(schedule.getLastDoneOdometer()).isEqualByComparingTo("15000");
    }

    @Test
    void vehicleDocument_expiryDetection_AC3() {
        Vehicle vehicle = new Vehicle(APP_ID, "ABC-1234", "Toyota", "Hilux", 2024, Vehicle.VehicleType.TRUCK, null, null, null, BigDecimal.ZERO, null);
        when(vehicleRepository.findByAppIdAndId(APP_ID, vehicle.getId())).thenReturn(Optional.of(vehicle));

        // Expired document
        VehicleDocument expiredDoc = new VehicleDocument(
                APP_ID, vehicle.getId(), VehicleDocument.DocumentType.INSURANCE, "POL-999",
                "2025-01-01", LocalDate.now().minusDays(5).toString(), "Allianz", null
        );

        when(documentRepository.save(any(VehicleDocument.class))).thenReturn(expiredDoc);

        FleetApi.VehicleDocumentCreateRequest docReq = new FleetApi.VehicleDocumentCreateRequest(
                vehicle.getId(),
                VehicleDocument.DocumentType.INSURANCE,
                "POL-999",
                "2025-01-01",
                LocalDate.now().minusDays(5).toString(),
                "Allianz",
                null
        );

        FleetApi.VehicleDocumentResponse res = fleetService.addDocument(docReq);
        assertThat(res.isExpired()).isTrue();
    }

    @Test
    void fleetCostSummary_aggregatesTotalsAccurately_AC5() {
        Vehicle v1 = new Vehicle(APP_ID, "ABC-1", "Make", "Model", 2023, Vehicle.VehicleType.SEDAN, null, null, null, new BigDecimal("20000"), null);
        Vehicle v2 = new Vehicle(APP_ID, "ABC-2", "Make", "Model", 2024, Vehicle.VehicleType.VAN, null, null, null, new BigDecimal("30000"), null);
        when(vehicleRepository.findByAppIdOrderByCreatedAtDesc(APP_ID)).thenReturn(List.of(v1, v2));

        FuelLog f1 = new FuelLog(APP_ID, v1.getId(), "2026-08-01", new BigDecimal("40"), new BigDecimal("10000"), new BigDecimal("400"), null, null, null);
        FuelLog f2 = new FuelLog(APP_ID, v2.getId(), "2026-08-05", new BigDecimal("60"), new BigDecimal("15000"), new BigDecimal("600"), null, null, null);
        when(fuelLogRepository.findByAppIdOrderByCreatedAtDesc(APP_ID)).thenReturn(List.of(f1, f2));

        MaintenanceRecord m1 = new MaintenanceRecord(APP_ID, v1.getId(), null, "Brakes", "2026-08-10", new BigDecimal("10000"), new BigDecimal("500"), null, null, null);
        when(recordRepository.findByAppIdOrderByPerformedDateDesc(APP_ID)).thenReturn(List.of(m1));

        FleetApi.FleetCostSummary summary = fleetService.getCostSummary();
        assertThat(summary.totalVehicles()).isEqualTo(2);
        assertThat(summary.totalFuelCost()).isEqualByComparingTo("1000"); // 400 + 600

        assertThat(summary.totalMaintenanceCost()).isEqualByComparingTo("500");
        assertThat(summary.grandTotalCost()).isEqualByComparingTo("1500"); // 1000 + 500
        assertThat(summary.totalKilometers()).isEqualByComparingTo("50000"); // 20000 + 30000
        // 1500 / 50000 = 0.03
        assertThat(summary.costPerKilometer()).isEqualByComparingTo("0.03");
    }
}
