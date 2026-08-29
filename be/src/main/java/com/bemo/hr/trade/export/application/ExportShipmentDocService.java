package com.bemo.hr.trade.export.application;

import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.export.domain.ComplianceRegister;
import com.bemo.hr.trade.export.domain.ExportShipment;
import com.bemo.hr.trade.export.domain.ExportShipmentLine;
import com.bemo.hr.trade.export.infrastructure.ComplianceRegisterRepository;
import com.bemo.hr.trade.export.infrastructure.ExportShipmentRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates printable export documents (Certificate of Origin, packing list,
 * phytosanitary application sheet) as localized xlsx workbooks.
 *
 * <p>All quantities, weights and package counts are read directly from the
 * shipment's persisted lines - there is no manual re-entry path, so the printed
 * quantities always match the lot quantities on the shipment (AC-1).</p>
 *
 * <p>The workbook is rendered from the bilingual translation catalog so the same
 * generator prints documents correctly in both Arabic (right-to-left) and English
 * (left-to-right) (AC-5).</p>
 */
@Service
@Transactional(readOnly = true)
public class ExportShipmentDocService {

    public enum DocType {
        COO("coo", "export.coo"),
        PACKING_LIST("packing-list", "export.packingList"),
        PHYTOSANITARY("phytosanitary", "export.phyto");

        private final String routeSegment;
        private final String titleKey;

        DocType(String routeSegment, String titleKey) {
            this.routeSegment = routeSegment;
            this.titleKey = titleKey;
        }

        public String routeSegment() {
            return routeSegment;
        }

        public String titleKey() {
            return titleKey;
        }

        public static DocType fromRoute(String segment) {
            for (DocType type : values()) {
                if (type.routeSegment.equals(segment)) return type;
            }
            throw new BusinessRuleException(
                    "Unsupported export document type: " + segment,
                    "EXPORT_DOC_TYPE_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    private final ExportShipmentRepository shipmentRepository;
    private final ComplianceRegisterRepository complianceRegisterRepository;
    private final TranslationService translationService;

    public ExportShipmentDocService(ExportShipmentRepository shipmentRepository,
                                    ComplianceRegisterRepository complianceRegisterRepository,
                                    TranslationService translationService) {
        this.shipmentRepository = shipmentRepository;
        this.complianceRegisterRepository = complianceRegisterRepository;
        this.translationService = translationService;
    }

    public byte[] render(String shipmentId, DocType type, String locale) {
        ExportShipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Export shipment not found", "EXPORT_SHIPMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        var options = new ExcelExportOptions(locale, null);
        var messages = ExcelExportSupport.messages(translationService, options);

        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = ExcelExportSupport.sheet(workbook,
                    ExcelExportSupport.text(messages, type.titleKey()), options.rightToLeft());
            var titleStyle = titleStyle(workbook);
            var labelStyle = labelStyle(workbook);
            var styles = ExcelExportSupport.styles(workbook);

            int row = 0;
            writeTitle(sheet, row++, messages, type.titleKey(), titleStyle, styles);

            row = writeMetaBlock(sheet, row, shipment, messages, labelStyle, styles);

            row = writeLinesTable(sheet, row, shipment, type, messages, styles);

            if (type == DocType.PHYTOSANITARY) {
                row = writeTreatmentsTable(sheet, row, shipment, messages, styles);
            }

            writeFooter(sheet, row + 1, messages, labelStyle);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create export document workbook.", exception);
        }
    }

    private void writeTitle(XSSFSheet sheet, int rowIndex, Map<String, String> messages,
                            String titleKey, CellStyle titleStyle, ExcelExportSupport.Styles styles) {
        var row = sheet.createRow(rowIndex);
        row.setHeightInPoints(28);
        row.createCell(0).setCellValue(ExcelExportSupport.text(messages, titleKey));
        row.getCell(0).setCellStyle(titleStyle);
    }

    private int writeMetaBlock(XSSFSheet sheet, int startRow, ExportShipment shipment,
                               Map<String, String> messages, CellStyle labelStyle,
                               ExcelExportSupport.Styles styles) {
        List<String> leftKeys = List.of(
                "export.shipmentNumber", "export.customer", "export.contractRef",
                "export.containerNo", "export.bookingNo", "export.acidNo");
        List<String> rightKeys = List.of(
                "export.portOfLoading", "export.portOfDischarge", "export.etbDate", "export.etaDate");

        String[][] leftValues = {
                {shipment.getShipmentNumber()},
                {shipment.getCustomerPartyName() != null ? shipment.getCustomerPartyName() : shipment.getCustomerPartyId()},
                {shipment.getContractRef()},
                {shipment.getContainerNo()},
                {shipment.getBookingNo()},
                {shipment.getAcidNo()},
        };
        String[] rightValues = {
                shipment.getPortOfLoading(),
                shipment.getPortOfDischarge(),
                shipment.getEtbDate() != null ? isoDate(shipment.getEtbDate()) : "",
                shipment.getEtaDate() != null ? isoDate(shipment.getEtaDate()) : "",
        };

        int rowIndex = startRow;
        for (int i = 0; i < leftKeys.size(); i++) {
            writeLabelValue(sheet, rowIndex, leftKeys.get(i), leftValues[i][0], messages, labelStyle);
            if (i < rightKeys.size()) {
                writeLabelValueRight(sheet, rowIndex, rightKeys.get(i), rightValues[i], messages, labelStyle);
            }
            rowIndex++;
        }
        return rowIndex + 1;
    }

    private void writeLabelValue(XSSFSheet sheet, int rowIndex, String key, String value,
                                 Map<String, String> messages, CellStyle labelStyle) {
        var row = sheet.getRow(rowIndex) != null ? sheet.getRow(rowIndex) : sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(ExcelExportSupport.text(messages, key));
        row.getCell(0).setCellStyle(labelStyle);
        row.createCell(1).setCellValue(ExcelExportSupport.escapeFormula(value == null ? "" : value));
        row.getCell(1).setCellStyle(labelStyle);
    }

    private void writeLabelValueRight(XSSFSheet sheet, int rowIndex, String key, String value,
                                      Map<String, String> messages, CellStyle labelStyle) {
        var row = sheet.getRow(rowIndex) != null ? sheet.getRow(rowIndex) : sheet.createRow(rowIndex);
        row.createCell(3).setCellValue(ExcelExportSupport.text(messages, key));
        row.getCell(3).setCellStyle(labelStyle);
        row.createCell(4).setCellValue(ExcelExportSupport.escapeFormula(value == null ? "" : value));
        row.getCell(4).setCellStyle(labelStyle);
    }

    private int writeLinesTable(XSSFSheet sheet, int startRow, ExportShipment shipment, DocType type,
                                Map<String, String> messages, ExcelExportSupport.Styles styles) {
        List<String> headerKeys = List.of(
                "export.doc.colNo",
                "export.column.itemName",
                "export.column.itemCode",
                "export.lotReference",
                "export.quantity",
                "export.unitOfMeasure",
                "export.netWeight",
                "export.grossWeight",
                "export.packagesCount");

        var headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(22);
        for (int column = 0; column < headerKeys.size(); column++) {
            headerRow.createCell(column).setCellValue(
                    ExcelExportSupport.text(messages, headerKeys.get(column)));
            headerRow.getCell(column).setCellStyle(styles.text());
        }

        int rowIndex = startRow + 1;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        int totalPackages = 0;

        for (ExportShipmentLine line : shipment.getLines()) {
            BigDecimal qty = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            BigDecimal net = line.getNetWeightKg() == null ? BigDecimal.ZERO : line.getNetWeightKg();
            BigDecimal gross = line.getGrossWeightKg() == null ? BigDecimal.ZERO : line.getGrossWeightKg();
            int packages = line.getPackagesCount() == null ? 0 : line.getPackagesCount();

            totalQty = totalQty.add(qty);
            totalNet = totalNet.add(net);
            totalGross = totalGross.add(gross);
            totalPackages += packages;

            List<?> values = List.of(
                    line.getLineOrder(),
                    line.getItemName(),
                    line.getItemCode() == null ? "" : line.getItemCode(),
                    line.getLotReference() == null ? "" : line.getLotReference(),
                    qty,
                    line.getUnitOfMeasure() == null ? "" : line.getUnitOfMeasure(),
                    net,
                    gross,
                    packages);
            ExcelExportSupport.writeRow(sheet, rowIndex++, values, styles);
        }

        List<?> totalValues = List.of(
                ExcelExportSupport.text(messages, "export.doc.total"),
                "", "", "", totalQty, "", totalNet, totalGross, totalPackages);
        ExcelExportSupport.writeRow(sheet, rowIndex++, totalValues, styles);

        return rowIndex + 1;
    }

    private int writeTreatmentsTable(XSSFSheet sheet, int startRow, ExportShipment shipment,
                                     Map<String, String> messages, ExcelExportSupport.Styles styles) {
        List<String> headerKeys = List.of(
                "export.lotReference",
                "export.chemical",
                "export.dose",
                "export.treatmentDate",
                "export.phiDays",
                "export.earliestSafePickup");

        var headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(22);
        for (int column = 0; column < headerKeys.size(); column++) {
            headerRow.createCell(column).setCellValue(
                    ExcelExportSupport.text(messages, headerKeys.get(column)));
            headerRow.getCell(column).setCellStyle(styles.text());
        }

        int rowIndex = startRow + 1;
        var seen = new java.util.HashSet<String>();
        for (ExportShipmentLine line : shipment.getLines()) {
            String lotRef = line.getLotReference();
            if (lotRef == null || lotRef.isBlank() || !seen.add(lotRef)) continue;
            List<ComplianceRegister> logs =
                    complianceRegisterRepository.findByLotReferenceOrderByTreatmentDateDesc(lotRef);
            if (logs.isEmpty()) {
                ExcelExportSupport.writeRow(sheet, rowIndex++,
                        List.of(lotRef, "", "", "", 0, ""), styles);
                continue;
            }
            for (ComplianceRegister log : logs) {
                ExcelExportSupport.writeRow(sheet, rowIndex++,
                        List.of(lotRef,
                                log.getChemical(),
                                log.getDose() == null ? "" : log.getDose(),
                                log.getTreatmentDate(),
                                log.getPreHarvestIntervalDays(),
                                log.earliestSafePickup()), styles);
            }
        }
        return rowIndex + 1;
    }

    private void writeFooter(XSSFSheet sheet, int rowIndex, Map<String, String> messages,
                             CellStyle labelStyle) {
        var row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(ExcelExportSupport.text(messages, "export.doc.issueDate"));
        row.getCell(0).setCellStyle(labelStyle);
        row.createCell(1).setCellValue(
                LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE));
        row.getCell(1).setCellStyle(labelStyle);
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle labelStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String isoDate(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC)
                .toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}