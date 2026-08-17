package com.bemo.hr.operations;

import com.bemo.hr.attendance.domain.BiometricSource;
import com.bemo.hr.attendance.domain.ImportBatch;
import com.bemo.hr.attendance.domain.PunchRecord;
import com.bemo.hr.attendance.infrastructure.BiometricSourceRepository;
import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.attendance.infrastructure.PunchRecordRepository;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile({"dev", "demo"})
@RequiredArgsConstructor
public class DemoScenarioDataService {
    private static final String DEMO_CHECKSUM = "4d07d64bf6a12b134ad0eaee56d79ee6a7283f61a86aee622f1ff1a5803b12e5";
    private static final String DEMO_SOURCE_NORMALIZED = "demo_biometric_device";
    private final AttendanceCategoryRepository attendanceCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final BiometricSourceRepository biometricSourceRepository;
    private final ImportBatchRepository importBatchRepository;
    private final PunchRecordRepository punchRecordRepository;
    private final BusinessPartyRepository businessPartyRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PartnerLedgerEntryRepository partnerLedgerEntryRepository;
    private final EmployeeAdvanceEntryRepository employeeAdvanceEntryRepository;
    @Value("${hr.company-zone:Africa/Cairo}")
    private String companyZone;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureDemoScenarios() {
        var administration = category("ADMINISTRATION");
        administration.configureAdvanceEligibility(true);
        var accounting = category("ACCOUNTING");
        accounting.configureAdvanceEligibility(true);
        var security = category("SECURITY");
        var daily = category("DAILY_WORKERS");
        var admin = employee("ADMINISTRATION-0001", "أحمد الإداري", "DEMO-ADMIN-1", administration.getId(), EmploymentType.FIXED);
        var accountant = employee("ACCOUNTING-0001", "منى المحاسبة", "DEMO-ACCOUNT-1", accounting.getId(), EmploymentType.FIXED);
        var guard = employee("SECURITY-0001", "محمود الأمن", "DEMO-SECURITY-1", security.getId(), EmploymentType.FIXED);
        employee("DAILY_WORKERS-0001", "يوسف العامل اليومي", null, daily.getId(), EmploymentType.DAILY);
        seedPunches(admin, accountant, guard);
        seedCommercialData(admin);
    }

    private com.bemo.hr.employee.domain.AttendanceCategory category(String code) {
        return attendanceCategoryRepository.findByCodeIgnoreCase(code).orElseThrow();
    }

    private Employee employee(String code, String name, String device, String categoryId, EmploymentType type) {
        return employeeRepository.findByEmployeeCodeIgnoreCase(code).orElseGet(() -> employeeRepository.save(
                new Employee(code, name, device, categoryId, type, LocalDate.of(2026, 1, 1), null, true)));
    }

    private void seedPunches(Employee admin, Employee accountant, Employee guard) {
        if (importBatchRepository.findBySourceIdAndChecksum(DEMO_SOURCE_NORMALIZED, DEMO_CHECKSUM).isPresent()) return;
        var workDays = firstWorkDays(YearMonth.now(ZoneId.of(companyZone)), 4);
        var rows = new ArrayList<PunchSeed>();
        rows.add(new PunchSeed(admin, workDays.get(0), LocalTime.of(8, 4)));
        rows.add(new PunchSeed(admin, workDays.get(0), LocalTime.of(16, 12)));
        rows.add(new PunchSeed(accountant, workDays.get(0), LocalTime.of(7, 58)));
        rows.add(new PunchSeed(accountant, workDays.get(0), LocalTime.of(18, 9)));
        rows.add(new PunchSeed(guard, workDays.get(0), LocalTime.of(8, 1)));
        rows.add(new PunchSeed(admin, workDays.get(1), LocalTime.of(8, 35)));
        rows.add(new PunchSeed(admin, workDays.get(1), LocalTime.of(17, 3)));
        rows.add(new PunchSeed(accountant, workDays.get(1), LocalTime.of(8, 2)));
        rows.add(new PunchSeed(guard, workDays.get(1), LocalTime.of(8, 0)));
        rows.add(new PunchSeed(guard, workDays.get(1), LocalTime.of(20, 5)));
        // No security punches on day three: report generation proposes a category holiday.
        BiometricSource source = biometricSourceRepository.findBySourceTypeAndNormalizedCode(
                        BiometricSource.SourceType.FILE_DEVICE, DEMO_SOURCE_NORMALIZED)
                .orElseGet(() -> biometricSourceRepository.save(new BiometricSource(
                        BiometricSource.SourceType.FILE_DEVICE, "Demo biometric device", DEMO_SOURCE_NORMALIZED)));
        var batch = importBatchRepository.save(new ImportBatch(DEMO_CHECKSUM, "demo-attendance-cases.xlsx",
                source.getId(), "Demo biometric device", "demo-seed", rows.size(), rows.size(), 0, rows.size(), 0));
        int row = 2;
        var zone = ZoneId.of(companyZone);
        for (var seed : rows)
            punchRecordRepository.save(new PunchRecord(batch.getId(), null, source.getId(),
                    seed.employee().getId(), seed.employee().getDeviceUserId(),
                    seed.employee().getFullName(), seed.date().atTime(seed.time()).atZone(zone).toInstant(), "demo", row++));
    }

    private List<LocalDate> firstWorkDays(YearMonth month, int count) {
        var result = new ArrayList<LocalDate>();
        for (var date = month.atDay(1); result.size() < count && !date.isAfter(month.atEndOfMonth()); date = date.plusDays(1))
            if (date.getDayOfWeek() != DayOfWeek.FRIDAY) result.add(date);
        return result;
    }

    private void seedCommercialData(Employee eligibleEmployee) {
        var supplier = party("SUP-DEMO", "مورد البرتقال والكرتون", "SUPPLIER");
        var processor = party("PROC-DEMO", "عميل تشغيل وفرز", "PROCESSING_CUSTOMER");
        var exporter = party("EXP-DEMO", "عميل تصدير خارجي", "EXPORT_CUSTOMER");
        var sorter = party("SORT-DEMO", "تاجر الفرزة", "SORTING_TRADER");
        var orange = item("ORANGE-RAW", "برتقال خام", "RAW_MATERIAL", "KG");
        var cartons = item("CARTON", "كرتون تعبئة", "PACKAGING", "BOX");
        var stretch = item("STRETCH", "رول استرتش", "PRODUCTION_SUPPLY", "ROLL");
        var sorted = item("SORTED-ORANGE", "برتقال فرزة", "SORTING_OUTPUT", "KG");
        if (stockMovementRepository.count() == 0) {
            Instant now = Instant.now();
            movement(orange, supplier, "SUPPLY_RECEIPT", "10000", "0", null, "PO-DEMO-001", now);
            movement(cartons, supplier, "SUPPLY_RECEIPT", "500", "0", null, "PO-DEMO-002", now.plusSeconds(1));
            movement(stretch, supplier, "SUPPLY_RECEIPT", "40", "0", null, "PO-DEMO-003", now.plusSeconds(2));
            movement(orange, processor, "PROCESSING_INTAKE", "2000", "0", "5", "PROC-DEMO-001", now.plusSeconds(3));
            movement(orange, exporter, "EXPORT_SALE", "-1000", "0", null, "EXP-DEMO-001", now.plusSeconds(4));
            movement(sorted, sorter, "SORTING_SALE", "-300", "0", null, "SORT-DEMO-001", now.plusSeconds(5));
            ledger(supplier, "SUPPLIER_INVOICE", "-50000", "PO-DEMO-001", now);
            ledger(exporter, "EXPORT_INVOICE", "80000", "EXP-DEMO-001", now.plusSeconds(4));
            ledger(sorter, "SORTING_SALE", "9000", "SORT-DEMO-001", now.plusSeconds(5));
        }
        if (employeeAdvanceEntryRepository.count() == 0)
            employeeAdvanceEntryRepository.save(new EmployeeAdvanceEntry(eligibleEmployee.getId(), new BigDecimal("1500"), "ADVANCE", "سلفة تجريبية", Instant.now(), "demo-seed"));
    }

    private BusinessParty party(String code, String name, String type) {
        return businessPartyRepository.findByCodeIgnoreCase(code).orElseGet(() -> businessPartyRepository.save(new BusinessParty(code, name, null, type, null, null, null, null, "بيانات تجريبية", true, "DIRECT", null, null, null, "EGP", "E_INVOICE", "CASH", null, null)));
    }

    private InventoryItem item(String code, String name, String type, String unit) {
        return inventoryItemRepository.findAll().stream().filter(value -> value.getCode().equalsIgnoreCase(code)).findFirst().orElseGet(() -> inventoryItemRepository.save(new InventoryItem(code, name, type, unit)));
    }

    private void movement(InventoryItem item, BusinessParty party, String type, String quantity, String amount, String loss, String reference, Instant at) {
        stockMovementRepository.save(new StockMovement(item.getId(), party.getId(), type, new BigDecimal(quantity), loss == null ? null : new BigDecimal(loss), reference, "بيانات تجريبية", at, "demo-seed"));
    }

    private void ledger(BusinessParty party, String type, String amount, String reference, Instant at) {
        partnerLedgerEntryRepository.save(new PartnerLedgerEntry(party.getId(), type, new BigDecimal(amount), reference, "بيانات تجريبية", at, "demo-seed"));
    }

    private record PunchSeed(Employee employee, LocalDate date, LocalTime time) {
    }
}
