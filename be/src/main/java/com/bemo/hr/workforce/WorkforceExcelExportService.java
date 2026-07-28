package com.bemo.hr.workforce;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkforceExcelExportService {

    private final WorkforceSettlementPeriodRepository periodRepository;
    private final WorkerSettlementRepository workerSettlementRepository;
    private final ContractorSettlementRepository contractorSettlementRepository;
    private final ManualAttendanceEntryRepository attendanceRepository;
    private final WorkerRepository workerRepository;
    private final ContractorRepository contractorRepository;
    private final WorkforceAdvanceRepository advanceRepository;

    public byte[] generatePeriodExcel(String periodId) throws IOException {
        WorkforceSettlementPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Period not found: " + periodId));

        List<ContractorSettlement> contractorSettlements = contractorSettlementRepository.findByPeriodId(periodId);
        List<WorkerSettlement> workerSettlements = workerSettlementRepository.findByPeriodId(periodId);
        List<ManualAttendanceEntry> attendanceEntries = attendanceRepository.findByWorkDateBetween(period.getStartDate(), period.getEndDate());
        List<Contractor> contractors = contractorRepository.findAll();
        List<Worker> workers = workerRepository.findAll();
        List<WorkforceAdvance> advances = advanceRepository.findAll();

        Map<String, Contractor> contractorMap = contractors.stream().collect(Collectors.toMap(Contractor::getId, c -> c));
        Map<String, Worker> workerMap = workers.stream().collect(Collectors.toMap(Worker::getId, w -> w));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Header & Data Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Calculate Period Days Range (e.g. 16..31)
            LocalDate start = LocalDate.parse(period.getStartDate());
            LocalDate end = LocalDate.parse(period.getEndDate());
            List<LocalDate> dateList = new ArrayList<>();
            LocalDate current = start;
            while (!current.isAfter(end)) {
                dateList.add(current);
                current = current.plusDays(1);
            }

            // ==========================================
            // SHEET 1: اجمالي راتب المدة (Summary Payroll)
            // ==========================================
            Sheet summarySheet = workbook.createSheet("اجمالي راتب المدة");
            summarySheet.setRightToLeft(true);

            Row titleRow1 = summarySheet.createRow(0);
            Cell titleCell1 = titleRow1.createCell(0);
            titleCell1.setCellValue("محطة الصديق لتصدير وفرز وتعبئة الحاصلات الزراعية");
            titleCell1.setCellStyle(titleStyle);

            Row titleRow2 = summarySheet.createRow(1);
            Cell titleCell2 = titleRow2.createCell(0);
            titleCell2.setCellValue("كشف مجمع لقبض العمالة — " + period.getPeriodCode() + " (" + period.getStartDate() + " إلى " + period.getEndDate() + ")");
            titleCell2.setCellStyle(titleStyle);

            Row hRow = summarySheet.createRow(3);
            String[] summaryHeaders = {"م", "اسم العمالة", "عدد الأيام", "الأجر", "المستحق", "الخصم", "الصافي بعد الخصم", "السلف", "الصافي بعد السلف"};
            for (int i = 0; i < summaryHeaders.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(summaryHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int sRowIdx = 4;
            int counter = 1;
            BigDecimal totalDays = BigDecimal.ZERO;
            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalDeductions = BigDecimal.ZERO;
            BigDecimal totalAdvances = BigDecimal.ZERO;
            BigDecimal totalNetPayable = BigDecimal.ZERO;

            for (ContractorSettlement cs : contractorSettlements) {
                Contractor contractor = contractorMap.get(cs.getContractorId());
                String contractorName = contractor != null ? contractor.getName() : "مقاول " + counter;

                Row row = summarySheet.createRow(sRowIdx++);
                createCell(row, 0, String.valueOf(counter++), dataStyle);
                createCell(row, 1, contractorName, dataStyle);

                // Calculate worker days for this contractor
                List<WorkerSettlement> cWsList = workerSettlements.stream()
                        .filter(ws -> cs.getContractorId().equals(ws.getContractorId())).toList();
                BigDecimal cDays = cWsList.stream().map(WorkerSettlement::getTotalAttendanceUnits).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal cDeductions = cs.getDeductionsAmount();
                BigDecimal cAdvances = cWsList.stream().map(WorkerSettlement::getAdvanceDeductions).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal cNetAfterDeductions = cs.getGrossAmount().subtract(cDeductions);

                createCell(row, 2, cDays.stripTrailingZeros().toPlainString(), dataStyle);
                createCell(row, 3, contractor != null && contractor.getDefaultDailyRate() != null ? contractor.getDefaultDailyRate().toString() : "-", dataStyle);
                createCell(row, 4, cs.getGrossAmount().toString(), dataStyle);
                createCell(row, 5, cDeductions.toString(), dataStyle);
                createCell(row, 6, cNetAfterDeductions.toString(), dataStyle);
                createCell(row, 7, cAdvances.toString(), dataStyle);
                createCell(row, 8, cs.getNetPayable().toString(), dataStyle);

                totalDays = totalDays.add(cDays);
                totalGross = totalGross.add(cs.getGrossAmount());
                totalDeductions = totalDeductions.add(cDeductions);
                totalAdvances = totalAdvances.add(cAdvances);
                totalNetPayable = totalNetPayable.add(cs.getNetPayable());
            }

            // Total Summary Row
            Row sumRow = summarySheet.createRow(sRowIdx);
            createCell(sumRow, 0, "الإجمالي", totalStyle);
            createCell(sumRow, 1, "", totalStyle);
            createCell(sumRow, 2, totalDays.stripTrailingZeros().toPlainString(), totalStyle);
            createCell(sumRow, 3, "", totalStyle);
            createCell(sumRow, 4, totalGross.toString(), totalStyle);
            createCell(sumRow, 5, totalDeductions.toString(), totalStyle);
            createCell(sumRow, 6, totalGross.subtract(totalDeductions).toString(), totalStyle);
            createCell(sumRow, 7, totalAdvances.toString(), totalStyle);
            createCell(sumRow, 8, totalNetPayable.toString(), totalStyle);

            for (int i = 0; i < summaryHeaders.length; i++) {
                summarySheet.autoSizeColumn(i);
            }

            // ==========================================
            // SHEETS 2..N: Contractor Sheets (Per Contractor)
            // ==========================================
            Map<String, List<Worker>> workersByContractor = workers.stream()
                    .collect(Collectors.groupingBy(Worker::getContractorId));

            for (Contractor c : contractors) {
                List<Worker> cWorkers = workersByContractor.get(c.getId());
                if (cWorkers == null || cWorkers.isEmpty()) continue;

                String safeSheetName = sanitizeSheetName(c.getName());
                Sheet cSheet = workbook.createSheet(safeSheetName);
                cSheet.setRightToLeft(true);

                Row cTitleRow = cSheet.createRow(0);
                Cell cTitleCell = cTitleRow.createCell(0);
                cTitleCell.setCellValue(c.getName() + " — يومية حضور وغياب عن الفترة " + period.getPeriodCode());
                cTitleCell.setCellStyle(titleStyle);

                // Days of week header row
                Row dayNamesRow = cSheet.createRow(2);
                createCell(dayNamesRow, 0, "", subHeaderStyle);
                for (int d = 0; d < dateList.size(); d++) {
                    createCell(dayNamesRow, d + 1, getArabicDayName(dateList.get(d)), subHeaderStyle);
                }

                // Date numbers header row
                Row dateNumsRow = cSheet.createRow(3);
                createCell(dateNumsRow, 0, "الاسم", headerStyle);
                for (int d = 0; d < dateList.size(); d++) {
                    createCell(dateNumsRow, d + 1, String.valueOf(dateList.get(d).getDayOfMonth()), headerStyle);
                }
                int colOffset = dateList.size() + 1;
                String[] calcHeaders = {"اجمالي الايام", "اجر اليوم", "المستحق", "اجر الساعة", "خصومات", "سلف", "صافى الراتب", "المديونية / ملاحظات"};
                for (int i = 0; i < calcHeaders.length; i++) {
                    createCell(dateNumsRow, colOffset + i, calcHeaders[i], headerStyle);
                }

                int wRowIdx = 4;
                BigDecimal[] dailyTotals = new BigDecimal[dateList.size()];
                for (int i = 0; i < dailyTotals.length; i++) dailyTotals[i] = BigDecimal.ZERO;

                BigDecimal cSheetTotalDays = BigDecimal.ZERO;
                BigDecimal cSheetTotalGross = BigDecimal.ZERO;
                BigDecimal cSheetTotalDeductions = BigDecimal.ZERO;
                BigDecimal cSheetTotalAdvances = BigDecimal.ZERO;
                BigDecimal cSheetTotalNet = BigDecimal.ZERO;

                for (Worker w : cWorkers) {
                    WorkerSettlement ws = workerSettlements.stream()
                            .filter(s -> s.getWorkerId().equals(w.getId())).findFirst().orElse(null);
                    if (ws == null) continue;

                    Map<String, BigDecimal> workerAttByDate = attendanceEntries.stream()
                            .filter(a -> a.getWorkerId().equals(w.getId()))
                            .collect(Collectors.toMap(ManualAttendanceEntry::getWorkDate, ManualAttendanceEntry::getAttendanceValue, (v1, v2) -> v1));

                    Row wRow = cSheet.createRow(wRowIdx++);
                    createCell(wRow, 0, w.getFullName(), dataStyle);

                    BigDecimal wDays = BigDecimal.ZERO;
                    for (int d = 0; d < dateList.size(); d++) {
                        String dateStr = dateList.get(d).toString();
                        BigDecimal val = workerAttByDate.getOrDefault(dateStr, BigDecimal.ZERO);
                        if (val.compareTo(BigDecimal.ZERO) > 0) {
                            createCell(wRow, d + 1, val.stripTrailingZeros().toPlainString(), dataStyle);
                            dailyTotals[d] = dailyTotals[d].add(val);
                            wDays = wDays.add(val);
                        } else {
                            createCell(wRow, d + 1, "", dataStyle);
                        }
                    }

                    BigDecimal dailyRate = w.getDefaultDailyRate();
                    BigDecimal hourlyRate = dailyRate.divide(new BigDecimal("9"), 2, RoundingMode.HALF_UP);
                    BigDecimal gross = ws.getGrossAmount();
                    BigDecimal deductions = ws.getDeductionsAmount();
                    BigDecimal advDeduction = ws.getAdvanceDeductions();
                    BigDecimal net = ws.getNetAmount();

                    createCell(wRow, colOffset, wDays.stripTrailingZeros().toPlainString(), dataStyle);
                    createCell(wRow, colOffset + 1, dailyRate.toString(), dataStyle);
                    createCell(wRow, colOffset + 2, gross.toString(), dataStyle);
                    createCell(wRow, colOffset + 3, hourlyRate.toString(), dataStyle);
                    createCell(wRow, colOffset + 4, deductions.compareTo(BigDecimal.ZERO) > 0 ? deductions.toString() : "", dataStyle);
                    createCell(wRow, colOffset + 5, advDeduction.compareTo(BigDecimal.ZERO) > 0 ? advDeduction.toString() : "", dataStyle);
                    createCell(wRow, colOffset + 6, net.toString(), dataStyle);
                    createCell(wRow, colOffset + 7, "", dataStyle);

                    cSheetTotalDays = cSheetTotalDays.add(wDays);
                    cSheetTotalGross = cSheetTotalGross.add(gross);
                    cSheetTotalDeductions = cSheetTotalDeductions.add(deductions);
                    cSheetTotalAdvances = cSheetTotalAdvances.add(advDeduction);
                    cSheetTotalNet = cSheetTotalNet.add(net);
                }

                // Bottom Total Row per Contractor
                Row cTotRow = cSheet.createRow(wRowIdx);
                createCell(cTotRow, 0, "Total", totalStyle);
                for (int d = 0; d < dateList.size(); d++) {
                    createCell(cTotRow, d + 1, dailyTotals[d].stripTrailingZeros().toPlainString(), totalStyle);
                }
                createCell(cTotRow, colOffset, cSheetTotalDays.stripTrailingZeros().toPlainString(), totalStyle);
                createCell(cTotRow, colOffset + 1, "", totalStyle);
                createCell(cTotRow, colOffset + 2, cSheetTotalGross.toString(), totalStyle);
                createCell(cTotRow, colOffset + 3, "", totalStyle);
                createCell(cTotRow, colOffset + 4, cSheetTotalDeductions.toString(), totalStyle);
                createCell(cTotRow, colOffset + 5, cSheetTotalAdvances.toString(), totalStyle);
                createCell(cTotRow, colOffset + 6, cSheetTotalNet.toString(), totalStyle);
                createCell(cTotRow, colOffset + 7, "", totalStyle);
            }

            // ==========================================
            // SHEET N+1: اجمالى عدد العمالة اليومى (Daily Headcount Summary)
            // ==========================================
            Sheet countSheet = workbook.createSheet("اجمالى عدد العمالة اليومى");
            countSheet.setRightToLeft(true);

            Row hcTitleRow = countSheet.createRow(0);
            Cell hcTitleCell = hcTitleRow.createCell(0);
            hcTitleCell.setCellValue("كشف خاص بإجمالي عدد العمالة في اليوم — " + period.getPeriodCode());
            hcTitleCell.setCellStyle(titleStyle);

            Row hcHeaderRow = countSheet.createRow(2);
            createCell(hcHeaderRow, 0, "م", headerStyle);
            createCell(hcHeaderRow, 1, "اسم العمالة", headerStyle);
            for (int d = 0; d < dateList.size(); d++) {
                createCell(hcHeaderRow, d + 2, String.valueOf(dateList.get(d).getDayOfMonth()), headerStyle);
            }
            createCell(hcHeaderRow, dateList.size() + 2, "الإجمالي", headerStyle);

            int hcRowIdx = 3;
            int hcCounter = 1;
            BigDecimal[] grandDailyCounts = new BigDecimal[dateList.size()];
            for (int i = 0; i < grandDailyCounts.length; i++) grandDailyCounts[i] = BigDecimal.ZERO;
            BigDecimal grandTotalHeadcount = BigDecimal.ZERO;

            for (Contractor c : contractors) {
                List<Worker> cWorkers = workersByContractor.get(c.getId());
                if (cWorkers == null || cWorkers.isEmpty()) continue;

                Row row = countSheet.createRow(hcRowIdx++);
                createCell(row, 0, String.valueOf(hcCounter++), dataStyle);
                createCell(row, 1, c.getName(), dataStyle);

                BigDecimal contractorTotalDays = BigDecimal.ZERO;
                for (int d = 0; d < dateList.size(); d++) {
                    String dateStr = dateList.get(d).toString();
                    BigDecimal dateCount = attendanceEntries.stream()
                            .filter(a -> cWorkers.stream().anyMatch(w -> w.getId().equals(a.getWorkerId())) && dateStr.equals(a.getWorkDate()))
                            .map(ManualAttendanceEntry::getAttendanceValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    createCell(row, d + 2, dateCount.compareTo(BigDecimal.ZERO) > 0 ? dateCount.stripTrailingZeros().toPlainString() : "0", dataStyle);
                    grandDailyCounts[d] = grandDailyCounts[d].add(dateCount);
                    contractorTotalDays = contractorTotalDays.add(dateCount);
                }
                createCell(row, dateList.size() + 2, contractorTotalDays.stripTrailingZeros().toPlainString(), dataStyle);
                grandTotalHeadcount = grandTotalHeadcount.add(contractorTotalDays);
            }

            Row hcSumRow = countSheet.createRow(hcRowIdx);
            createCell(hcSumRow, 0, "الاجمـــــالي", totalStyle);
            createCell(hcSumRow, 1, "", totalStyle);
            for (int d = 0; d < dateList.size(); d++) {
                createCell(hcSumRow, d + 2, grandDailyCounts[d].stripTrailingZeros().toPlainString(), totalStyle);
            }
            createCell(hcSumRow, dateList.size() + 2, grandTotalHeadcount.stripTrailingZeros().toPlainString(), totalStyle);

            // ==========================================
            // SHEET N+2: سلف المدة (Period Advances Breakdown)
            // ==========================================
            Sheet advSheet = workbook.createSheet("سلف المدة");
            advSheet.setRightToLeft(true);

            Row advTitle = advSheet.createRow(0);
            Cell advTitleCell = advTitle.createCell(0);
            advTitleCell.setCellValue("كشف سلف العمالة والمقاولين عن الفترة " + period.getPeriodCode());
            advTitleCell.setCellStyle(titleStyle);

            Row advHeader = advSheet.createRow(2);
            String[] advHeaders = {"م", "الاسم", "نوع العمالة / المقاول", "التاريخ", "السلفة", "مديونية المدة السابقة", "التوريد", "الصافي"};
            for (int i = 0; i < advHeaders.length; i++) {
                createCell(advHeader, i, advHeaders[i], headerStyle);
            }

            int aRowIdx = 3;
            int aCounter = 1;
            BigDecimal totalAdvAmount = BigDecimal.ZERO;

            for (WorkforceAdvance adv : advances) {
                Worker worker = adv.getWorkerId() != null ? workerMap.get(adv.getWorkerId()) : null;
                Contractor contractor = adv.getContractorId() != null ? contractorMap.get(adv.getContractorId()) : null;
                String name = worker != null ? worker.getFullName() : (contractor != null ? contractor.getName() : "-");
                String groupName = contractor != null ? contractor.getName() : (worker != null && contractorMap.containsKey(worker.getContractorId()) ? contractorMap.get(worker.getContractorId()).getName() : "-");

                Row row = advSheet.createRow(aRowIdx++);
                createCell(row, 0, String.valueOf(aCounter++), dataStyle);
                createCell(row, 1, name, dataStyle);
                createCell(row, 2, groupName, dataStyle);
                createCell(row, 3, adv.getCreatedAt() != null ? DateTimeFormatter.ISO_LOCAL_DATE.format(adv.getCreatedAt().atZone(java.time.ZoneId.systemDefault())) : "-", dataStyle);
                createCell(row, 4, adv.getAmount().toString(), dataStyle);
                createCell(row, 5, "", dataStyle);
                createCell(row, 6, "", dataStyle);
                createCell(row, 7, adv.getRemainingBalance().toString(), dataStyle);

                totalAdvAmount = totalAdvAmount.add(adv.getAmount());
            }

            Row advSumRow = advSheet.createRow(aRowIdx);
            createCell(advSumRow, 0, "الإجمالي", totalStyle);
            createCell(advSumRow, 1, "", totalStyle);
            createCell(advSumRow, 2, "", totalStyle);
            createCell(advSumRow, 3, "", totalStyle);
            createCell(advSumRow, 4, totalAdvAmount.toString(), totalStyle);
            createCell(advSumRow, 5, "", totalStyle);
            createCell(advSumRow, 6, "", totalStyle);
            createCell(advSumRow, 7, totalAdvAmount.toString(), totalStyle);

            // ==========================================
            // SHEET N+3: استلام (Receipt Vouchers Sheet)
            // ==========================================
            Sheet recSheet = workbook.createSheet("استلام");
            recSheet.setRightToLeft(true);

            Row rTitle = recSheet.createRow(0);
            Cell rTitleCell = rTitle.createCell(0);
            rTitleCell.setCellValue("بيان استلام قبض الفترة " + period.getPeriodCode() + " — مقاولين العمالة محطة الصديق");
            rTitleCell.setCellStyle(titleStyle);

            Row rHeader = recSheet.createRow(1);
            String[] rHeaders = {"م", "الإسم", "التوقيع", "ملاحظات"};
            for (int i = 0; i < rHeaders.length; i++) {
                createCell(rHeader, i, rHeaders[i], headerStyle);
            }

            int rRowIdx = 2;
            int rCounter = 1;
            for (Contractor c : contractors) {
                Row row = recSheet.createRow(rRowIdx++);
                createCell(row, 0, String.valueOf(rCounter++), dataStyle);
                createCell(row, 1, c.getName(), dataStyle);
                createCell(row, 2, "", dataStyle);
                createCell(row, 3, "", dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createCell(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    private String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) return "Sheet";
        String clean = name.replaceAll("[:\\\\/?*\\[\\]]", " ").strip();
        return clean.length() > 30 ? clean.substring(0, 30) : clean;
    }

    private String getArabicDayName(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> "السبت";
            case SUNDAY -> "الأحد";
            case MONDAY -> "الإثنين";
            case TUESDAY -> "الثلاثاء";
            case WEDNESDAY -> "الأربعاء";
            case THURSDAY -> "الخميس";
            case FRIDAY -> "الجمعة";
        };
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSubHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private CellStyle createTotalStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
