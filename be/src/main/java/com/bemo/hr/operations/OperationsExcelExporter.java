package com.bemo.hr.operations;

import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.reporting.infrastructure.ExcelExportSupport;
import com.bemo.hr.shared.i18n.TranslationService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OperationsExcelExporter {
    private final TranslationService translationService;

    byte[] export(OperationsApi.Snapshot data, ExcelExportOptions options) {
        Map<String, String> messages = ExcelExportSupport.messages(translationService, options);
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            write(workbook, ExcelExportSupport.text(messages, "operations.stockBalances"),
                    "InventoryBalancesTable",
                    List.of("common.code", "common.name", "operations.itemType", "operations.unit", "operations.balance"),
                    data.items().stream().<List<?>>map(item -> List.of(
                            item.code(), item.name(), itemType(messages, item.itemType()),
                            item.unitCode(), item.currentBalance())).toList(), messages, options);
            write(workbook, ExcelExportSupport.text(messages, "operations.stockMovements"),
                    "StockMovementsTable",
                    List.of("operations.date", "operations.operation", "operations.item", "operations.party",
                            "operations.quantity", "operations.loss", "operations.reference"),
                    data.movements().stream().<List<?>>map(movement -> List.of(
                            movement.occurredAt(), operationType(messages, movement.operationType()),
                            movement.itemName(), blankIfNull(movement.partyName()), movement.quantityDelta(),
                            blankIfNull(movement.lossPercentage()), blankIfNull(movement.referenceCode()))).toList(),
                    messages, options);
            write(workbook, ExcelExportSupport.text(messages, "operations.partnerBalances"),
                    "PartnerBalancesTable",
                    List.of("common.code", "operations.party", "operations.partyType", "operations.financialBalance"),
                    data.partyBalances().stream().<List<?>>map(party -> List.of(
                            party.partyCode(), party.partyName(), partyType(messages, party.partyType()),
                            party.balance())).toList(), messages, options);
            write(workbook, ExcelExportSupport.text(messages, "operations.employeeAdvances"),
                    "EmployeeAdvancesTable",
                    List.of("common.code", "operations.employee", "operations.entry", "operations.amount",
                            "operations.balance", "operations.date"),
                    data.employeeAdvances().stream().<List<?>>map(advance -> List.of(
                            advance.employeeCode(), advance.employeeName(), advanceType(messages, advance.entryType()),
                            advance.amountDelta(), advance.currentBalance(), advance.occurredAt())).toList(),
                    messages, options);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("Could not create operations workbook.", error);
        }
    }

    private void write(XSSFWorkbook workbook, String name, String tableName, List<String> headerKeys,
                       List<? extends List<?>> rows, Map<String, String> messages, ExcelExportOptions options) {
        var sheet = ExcelExportSupport.sheet(workbook, name, options.rightToLeft());
        var headers = headerKeys.stream().map(key -> ExcelExportSupport.text(messages, key)).toList();
        ExcelExportSupport.writeHeader(sheet, headers);
        var styles = ExcelExportSupport.styles(workbook);
        int rowIndex = 1;
        for (var row : rows) {
            ExcelExportSupport.writeRow(sheet, rowIndex++, row, styles);
        }
        ExcelExportSupport.finishTable(sheet, rowIndex - 1, headers.size(), tableName, options);
    }

    private String itemType(Map<String, String> messages, String value) {
        String key = switch (value) {
            case "RAW_MATERIAL" -> "operations.rawMaterial";
            case "PACKAGING" -> "operations.packaging";
            case "PRODUCTION_SUPPLY" -> "operations.productionSupply";
            case "SORTING_OUTPUT" -> "operations.sortingOutput";
            case "FINISHED_GOOD" -> "operations.finishedGood";
            default -> null;
        };
        return translated(messages, key, value);
    }

    private String operationType(Map<String, String> messages, String value) {
        String key = switch (value) {
            case "SUPPLY_RECEIPT" -> "operations.operationType.supplyReceipt";
            case "PAYMENT" -> "operations.operationType.payment";
            case "PROCESSING_INTAKE" -> "operations.operationType.processingIntake";
            case "PROCESSING_DELIVERY" -> "operations.operationType.processingDelivery";
            case "EXPORT_SALE" -> "operations.operationType.exportSale";
            case "SORTING_SALE" -> "operations.operationType.sortingSale";
            case "ADJUSTMENT" -> "operations.operationType.adjustment";
            default -> null;
        };
        return translated(messages, key, value);
    }

    private String partyType(Map<String, String> messages, String value) {
        String key = switch (value) {
            case "SUPPLIER" -> "partyType.supplier";
            case "PROCESSING_CUSTOMER" -> "partyType.processingCustomer";
            case "EXPORT_CUSTOMER" -> "partyType.exportCustomer";
            case "SORTING_TRADER" -> "partyType.sortingTrader";
            case "FARM" -> "partyType.farm";
            case "OTHER" -> "partyType.other";
            default -> null;
        };
        return translated(messages, key, value);
    }

    private String advanceType(Map<String, String> messages, String value) {
        String key = "REPAYMENT".equals(value) ? "operations.advanceRepaid" : "operations.advancePaid";
        return ExcelExportSupport.text(messages, key);
    }

    private String translated(Map<String, String> messages, String key, String fallback) {
        return key == null ? fallback : ExcelExportSupport.text(messages, key);
    }

    private Object blankIfNull(Object value) {
        return value == null ? "" : value;
    }
}
