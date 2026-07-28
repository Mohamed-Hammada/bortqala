package com.bemo.hr.workforce;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WorkforceExcelExportTest {

    @Test
    @DisplayName("Should generate valid multi-sheet Excel workbook matching client structure")
    void testExcelExportGeneration() throws Exception {
        var periodRepo = Mockito.mock(WorkforceSettlementPeriodRepository.class);
        var workerSettlementRepo = Mockito.mock(WorkerSettlementRepository.class);
        var contractorSettlementRepo = Mockito.mock(ContractorSettlementRepository.class);
        var attendanceRepo = Mockito.mock(ManualAttendanceEntryRepository.class);
        var workerRepo = Mockito.mock(WorkerRepository.class);
        var contractorRepo = Mockito.mock(ContractorRepository.class);
        var advanceRepo = Mockito.mock(WorkforceAdvanceRepository.class);

        WorkforceExcelExportService exportService = new WorkforceExcelExportService(
                periodRepo, workerSettlementRepo, contractorSettlementRepo,
                attendanceRepo, workerRepo, contractorRepo, advanceRepo
        );

        String periodId = "p1";
        WorkforceSettlementPeriod period = new WorkforceSettlementPeriod("DEC-2025-P2", "2025-12-16", "2025-12-31", "HALF_MONTH", "DRAFT");
        when(periodRepo.findById(periodId)).thenReturn(Optional.of(period));

        Contractor c1 = new Contractor("C01", "المحطة", "المحطة", "01000000000", null, null, null, "worker_net_total", "contractor_full", 15, new BigDecimal("225"), "fixed", BigDecimal.ZERO, "gross", BigDecimal.ZERO, "ACTIVE", null);
        when(contractorRepo.findAll()).thenReturn(List.of(c1));

        Worker w1 = new Worker("W01", "احمد خالد", c1.getId(), "CAT01", new BigDecimal("240"), new BigDecimal("8.0"), null, "MANUAL", "ACTIVE", null, null, null);
        when(workerRepo.findAll()).thenReturn(List.of(w1));

        WorkerSettlement ws1 = new WorkerSettlement(periodId, w1.getId(), c1.getId(), new BigDecimal("13"), new BigDecimal("240"), new BigDecimal("3120"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("900"), new BigDecimal("2120"));
        when(workerSettlementRepo.findByPeriodId(periodId)).thenReturn(List.of(ws1));

        ContractorSettlement cs1 = new ContractorSettlement(periodId, c1.getId(), "worker_net_total", new BigDecimal("2120"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("3120"), new BigDecimal("2120"), BigDecimal.ZERO, "REVIEW");
        when(contractorSettlementRepo.findByPeriodId(periodId)).thenReturn(List.of(cs1));

        ManualAttendanceEntry e1 = new ManualAttendanceEntry(w1.getId(), "2025-12-16", new BigDecimal("1.0"), "08:00", "16:00", new BigDecimal("8.0"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("240"), "MANUAL", "OK");
        when(attendanceRepo.findByWorkDateBetween("2025-12-16", "2025-12-31")).thenReturn(List.of(e1));

        byte[] excelBytes = exportService.generatePeriodExcel(periodId);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertTrue(wb.getNumberOfSheets() >= 5);
            assertEquals("اجمالي راتب المدة", wb.getSheetName(0));
            assertEquals("المحطة", wb.getSheetName(1));
            assertEquals("اجمالى عدد العمالة اليومى", wb.getSheetName(wb.getNumberOfSheets() - 3));
            assertEquals("سلف المدة", wb.getSheetName(wb.getNumberOfSheets() - 2));
            assertEquals("استلام", wb.getSheetName(wb.getNumberOfSheets() - 1));
        }
    }
}
