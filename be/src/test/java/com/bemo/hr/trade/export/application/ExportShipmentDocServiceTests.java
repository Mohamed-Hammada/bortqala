package com.bemo.hr.trade.export.application;

import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.export.domain.ComplianceRegister;
import com.bemo.hr.trade.export.domain.ExportShipment;
import com.bemo.hr.trade.export.domain.ExportShipmentLine;
import com.bemo.hr.trade.export.domain.ExportShipmentStatus;
import com.bemo.hr.trade.export.infrastructure.ComplianceRegisterRepository;
import com.bemo.hr.trade.export.infrastructure.ExportShipmentRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportShipmentDocServiceTests {

    @Mock
    private ExportShipmentRepository shipmentRepository;
    @Mock
    private ComplianceRegisterRepository complianceRegisterRepository;
    @Mock
    private TranslationService translationService;

    private ExportShipmentDocService service;

    private static final Map<String, String> EN = new LinkedHashMap<>();
    private static final Map<String, String> AR = new LinkedHashMap<>();

    static {
        for (String key : List.of(
                "export.coo", "export.packingList", "export.phyto",
                "export.doc.colNo", "export.doc.total", "export.doc.issueDate",
                "export.shipmentNumber", "export.customer", "export.contractRef",
                "export.containerNo", "export.bookingNo", "export.acidNo",
                "export.portOfLoading", "export.portOfDischarge", "export.etbDate", "export.etaDate",
                "export.column.itemName", "export.column.itemCode",
                "export.lotReference", "export.quantity", "export.unitOfMeasure",
                "export.netWeight", "export.grossWeight", "export.packagesCount",
                "export.chemical", "export.dose", "export.treatmentDate",
                "export.phiDays", "export.earliestSafePickup")) {
            EN.put(key, "EN-" + key);
            AR.put(key, "AR-" + key);
        }
    }

    @BeforeEach
    void setUp() {
        service = new ExportShipmentDocService(shipmentRepository, complianceRegisterRepository, translationService);
        lenient().when(translationService.bundle("en-US"))
                .thenReturn(new TranslationService.TranslationBundle("en-US", null, Map.copyOf(EN)));
        lenient().when(translationService.bundle("ar-EG"))
                .thenReturn(new TranslationService.TranslationBundle("ar-EG", null, Map.copyOf(AR)));
        lenient().when(translationService.translateOrDefault(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(2));
    }

    @Test
    void render_coo_quantitiesMatchLotsExactly() throws Exception {
        ExportShipment shipment = shipmentWithThreeLots();
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        byte[] bytes = service.render("sh-1", ExportShipmentDocService.DocType.COO, "en-US");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            int qtyColumn = findColumn(sheet, "EN-export.quantity");
            assertThat(qtyColumn).isGreaterThanOrEqualTo(0);

            List<Double> dataQtys = dataRowQuantityValues(sheet, qtyColumn);
            assertThat(dataQtys).containsExactly(
                    1000.0, 500.0, 750.0);
        }
    }

    @Test
    void render_coo_totalRowSumsLots() throws Exception {
        ExportShipment shipment = shipmentWithThreeLots();
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        byte[] bytes = service.render("sh-1", ExportShipmentDocService.DocType.COO, "en-US");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            int qtyColumn = findColumn(sheet, "EN-export.quantity");
            int totalRow = findRowContaining(sheet, "EN-export.doc.total");
            assertThat(totalRow).isNotEqualTo(-1);
            assertThat(sheet.getRow(totalRow).getCell(qtyColumn).getNumericCellValue())
                    .isEqualTo(2250.0);
        }
    }

    @Test
    void render_packingList_totalsSumPerField() throws Exception {
        ExportShipment shipment = shipmentWithThreeLots();
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        byte[] bytes = service.render("sh-1", ExportShipmentDocService.DocType.PACKING_LIST, "en-US");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            int qtyCol = findColumn(sheet, "EN-export.quantity");
            int netCol = findColumn(sheet, "EN-export.netWeight");
            int pkgCol = findColumn(sheet, "EN-export.packagesCount");
            int totalRow = findRowContaining(sheet, "EN-export.doc.total");

            assertThat(sheet.getRow(totalRow).getCell(qtyCol).getNumericCellValue()).isEqualTo(2250.0);
            assertThat(sheet.getRow(totalRow).getCell(netCol).getNumericCellValue()).isEqualTo(2185.0);
            assertThat(sheet.getRow(totalRow).getCell(pkgCol).getNumericCellValue()).isEqualTo(19.0);
        }
    }

    @Test
    void render_arabic_rightToLeftSheetAndArabicHeaders() throws Exception {
        ExportShipment shipment = shipmentWithThreeLots();
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        byte[] bytes = service.render("sh-1", ExportShipmentDocService.DocType.PACKING_LIST, "ar-EG");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).isRightToLeft()).isTrue();
            int qtyColumn = findColumn(sheet, "AR-export.quantity");
            assertThat(qtyColumn).isGreaterThanOrEqualTo(0);
            Row header = sheet.getRow(findRowContaining(sheet, "AR-export.quantity"));
            assertThat(header.getCell(qtyColumn).getStringCellValue()).isEqualTo("AR-export.quantity");
        }
    }

    @Test
    void render_english_leftToRightSheet() throws Exception {
        ExportShipment shipment = shipmentWithThreeLots();
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));

        byte[] bytes = service.render("sh-1", ExportShipmentDocService.DocType.COO, "en-US");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetAt(0).isRightToLeft()).isFalse();
        }
    }

    @Test
    void render_phytosanitary_includesTreatmentsPerLot() throws Exception {
        ExportShipment shipment = shipmentWithThreeLots();
        when(shipmentRepository.findById("sh-1")).thenReturn(java.util.Optional.of(shipment));
        ComplianceRegister log = new ComplianceRegister("LOT-T1", "Chlorpyrifos",
                LocalDate.of(2026, 8, 1), 14);
        log.setDose("2 L/ha");
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-T1"))
                .thenReturn(List.of(log));
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-C1"))
                .thenReturn(List.of());
        when(complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc("LOT-O1"))
                .thenReturn(List.of());

        byte[] bytes = service.render("sh-1", ExportShipmentDocService.DocType.PHYTOSANITARY, "en-US");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            int chemicalCol = findColumn(sheet, "EN-export.chemical");
            assertThat(chemicalCol).isGreaterThanOrEqualTo(0);
            boolean found = false;
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                var cell = row.getCell(chemicalCol);
                if (cell != null && "Chlorpyrifos".equals(cell.getStringCellValue())) found = true;
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    void render_nonexistentShipment_throws() {
        when(shipmentRepository.findById("missing")).thenReturn(java.util.Optional.empty());
        try {
            service.render("missing", ExportShipmentDocService.DocType.COO, "en-US");
            assertThat(false).isTrue();
        } catch (com.bemo.hr.shared.domain.BusinessRuleException e) {
            assertThat(e.getCode()).isEqualTo("EXPORT_SHIPMENT_NOT_FOUND");
        }
    }

    @Test
    void docType_fromRoute_parsesSegments() {
        assertThat(ExportShipmentDocService.DocType.fromRoute("coo")).isEqualTo(ExportShipmentDocService.DocType.COO);
        assertThat(ExportShipmentDocService.DocType.fromRoute("packing-list"))
                .isEqualTo(ExportShipmentDocService.DocType.PACKING_LIST);
        assertThat(ExportShipmentDocService.DocType.fromRoute("phytosanitary"))
                .isEqualTo(ExportShipmentDocService.DocType.PHYTOSANITARY);
    }

    private ExportShipment shipmentWithThreeLots() {
        ExportShipment shipment = new ExportShipment("EXP-20260826-REX", "party-1", "GreenEx Corp");
        try {
            var field = ExportShipment.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(shipment, "sh-1");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        shipment.setStatus(ExportShipmentStatus.BOOKED);
        shipment.setContractRef("CTR-77");
        shipment.setContainerNo("MSKU-111");
        shipment.setBookingNo("BK-99");
        shipment.setAcidNo("ACID-2026");
        shipment.setPortOfLoading("Alexandria");
        shipment.setPortOfDischarge("Rotterdam");

        ExportShipmentLine l1 = new ExportShipmentLine(1, "Tomatoes", BigDecimal.valueOf(1000));
        l1.setLotReference("LOT-T1");
        l1.setUnitOfMeasure("KG");
        l1.setNetWeightKg(BigDecimal.valueOf(1000));
        l1.setGrossWeightKg(BigDecimal.valueOf(1050));
        l1.setPackagesCount(10);
        ExportShipmentLine l2 = new ExportShipmentLine(2, "Cucumbers", BigDecimal.valueOf(500));
        l2.setLotReference("LOT-C1");
        l2.setUnitOfMeasure("KG");
        l2.setNetWeightKg(BigDecimal.valueOf(500));
        l2.setGrossWeightKg(BigDecimal.valueOf(515));
        l2.setPackagesCount(5);
        ExportShipmentLine l3 = new ExportShipmentLine(3, "Onions", BigDecimal.valueOf(750));
        l3.setLotReference("LOT-O1");
        l3.setUnitOfMeasure("KG");
        l3.setNetWeightKg(BigDecimal.valueOf(685));
        l3.setGrossWeightKg(BigDecimal.valueOf(700));
        l3.setPackagesCount(4);

        shipment.addLine(l1);
        shipment.addLine(l2);
        shipment.addLine(l3);
        return shipment;
    }

    private int findColumn(Sheet sheet, String headerValue) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                var cell = row.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && headerValue.equals(cell.getStringCellValue())) {
                    return c;
                }
            }
        }
        return -1;
    }

    private int findRowContaining(Sheet sheet, String value) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                var cell = row.getCell(c);
                if (cell != null && cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING
                        && value.equals(cell.getStringCellValue())) {
                    return r;
                }
            }
        }
        return -1;
    }

    private List<Double> dataRowQuantityValues(Sheet sheet, int qtyColumn) {
        int totalRowIndex = findRowContaining(sheet, "EN-export.doc.total");
        java.util.ArrayList<Double> values = new java.util.ArrayList<>();
        for (int r = 1; r < totalRowIndex; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            var cell = row.getCell(qtyColumn);
            if (cell == null) continue;
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                values.add(cell.getNumericCellValue());
            }
        }
        return values;
    }
}