package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.api.PayrollApi;
import com.bemo.hr.payroll.domain.PaymentMethod;
import com.bemo.hr.payroll.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayrollWpsExportServiceTests {

    private PayrollWpsExportService service;
    private PayrollApi.SheetResponse sampleSheet;

    @BeforeEach
    void setUp() {
        service = new PayrollWpsExportService();
        var row1 = new PayrollApi.PayrollRow(
                "p-1", "emp-1", "EMP001", "Mohamed Ali", "cat-1", "Engineers", "FULL_TIME",
                "rep-1", 2026, 8, "FULL_MONTH", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("10000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), BigDecimal.ZERO,
                new BigDecimal("9000.00"), PaymentStatus.APPROVED, Instant.now(), PaymentMethod.BANK_TRANSFER,
                "REF-001", "August Salary", false, "system", Instant.now(), null, null, null, null, 1L
        );
        var summary = new PayrollApi.Summary(1, 1, 0, new BigDecimal("10000.00"), new BigDecimal("9000.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        sampleSheet = new PayrollApi.SheetResponse(2026, 8, PaymentStatus.APPROVED, summary, List.of(row1));
    }

    @Test
    void generateEgWpsCsv_createsValidCsvContentWithBomAndHeaders() {
        byte[] bytes = service.generateWpsFile(sampleSheet, PayrollWpsExportService.WpsFormat.EG_WPS, "TAX-123456", "CIBEGXXX");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String csvText = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("Egyptian WPS / ACH Clearing File"));
        assertTrue(csvText.contains("TAX-123456"));
        assertTrue(csvText.contains("CIBEGXXX"));
        assertTrue(csvText.contains("EMP001"));
        assertTrue(csvText.contains("Mohamed Ali"));
        assertTrue(csvText.contains("9000.00"));
    }

    @Test
    void generateGccSif_createsValidScrAndEdrRecords() {
        byte[] bytes = service.generateWpsFile(sampleSheet, PayrollWpsExportService.WpsFormat.GCC_SIF, "CORP-999", "NBE01");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String sifText = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(sifText.startsWith("SCR,CORP-999,NBE01,"));
        assertTrue(sifText.contains("202608,1,9000.00,EGP"));
        assertTrue(sifText.contains("EDR,EMP001,NBE01,"));
        assertTrue(sifText.contains("20260801,20260831,31,10000.00,0.00,1000.00,9000.00"));
    }
}
