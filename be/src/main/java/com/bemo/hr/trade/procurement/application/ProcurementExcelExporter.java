package com.bemo.hr.trade.procurement.application;

import com.bemo.hr.trade.procurement.api.ProcurementApi;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

@Component
public class ProcurementExcelExporter {
    public byte[] export(List<ProcurementApi.PurchaseOrderResponse> orders,
                         List<ProcurementApi.GoodsReceiptResponse> receipts,
                         List<ProcurementApi.SupplierInvoiceResponse> invoices,
                         List<ProcurementApi.SupplierPaymentResponse> payments,
                         String locale, String actor) {
        boolean arabic = locale != null && locale.startsWith("ar");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle header = workbook.createCellStyle();
            header.setAlignment(HorizontalAlignment.CENTER);
            var font = workbook.createFont(); font.setBold(true); header.setFont(font);

            var orderSheet = workbook.createSheet(arabic ? "أوامر الشراء" : "Purchase Orders");
            orderSheet.setRightToLeft(arabic);
            metadata(orderSheet, arabic, actor);
            row(orderSheet, 2, header, arabic ? new String[]{"رقم الأمر","التاريخ","المورد","العملة","الحالة","الإجمالي"}
                    : new String[]{"PO Number","Date","Supplier","Currency","Status","Total"});
            int index = 3;
            for (var value : orders) {
                var row = orderSheet.createRow(index++);
                text(row, 0, value.poNumber()); date(row, 1, value.poDate()); text(row, 2, value.supplierName());
                text(row, 3, value.currencyCode()); text(row, 4, value.status()); number(row, 5, value.totalAmount());
            }
            finish(orderSheet, 6);

            var receiptSheet = workbook.createSheet(arabic ? "إيصالات البضائع" : "Goods Receipts");
            receiptSheet.setRightToLeft(arabic); metadata(receiptSheet, arabic, actor);
            row(receiptSheet, 2, header, arabic ? new String[]{"رقم الإيصال","التاريخ","المورد","أمر الشراء","الحالة"}
                    : new String[]{"GRN Number","Date","Supplier","Purchase Order","Status"});
            index = 3;
            for (var value : receipts) {
                var row = receiptSheet.createRow(index++);
                text(row, 0, value.grnNumber()); date(row, 1, value.receiptDate()); text(row, 2, value.supplierName());
                text(row, 3, value.purchaseOrderId()); text(row, 4, value.status());
            }
            finish(receiptSheet, 5);

            var invoiceSheet = workbook.createSheet(arabic ? "فواتير الموردين" : "Supplier Invoices");
            invoiceSheet.setRightToLeft(arabic); metadata(invoiceSheet, arabic, actor);
            row(invoiceSheet, 2, header, arabic ? new String[]{"رقم الفاتورة","المرجع الداخلي","التاريخ","المورد","العملة","الأصلي","الخصم","الضريبة","الصافي","المدفوع","المتبقي","الحالة"}
                    : new String[]{"Invoice","Internal Reference","Date","Supplier","Currency","Original","Discount","Tax","Net","Paid","Outstanding","Status"});
            index = 3;
            for (var value : invoices) {
                var row = invoiceSheet.createRow(index++);
                text(row, 0, value.invoiceNumber()); text(row, 1, value.internalReference()); date(row, 2, value.invoiceDate());
                text(row, 3, value.supplierName()); text(row, 4, value.currencyCode());
                number(row, 5, value.totalAmount()); number(row, 6, value.discountAmount()); number(row, 7, value.taxAmount());
                number(row, 8, value.netAmount()); number(row, 9, value.paidAmount()); number(row, 10, value.outstandingAmount());
                text(row, 11, value.status());
            }
            finish(invoiceSheet, 12);

            var paymentSheet = workbook.createSheet(arabic ? "مدفوعات الموردين" : "Supplier Payments");
            paymentSheet.setRightToLeft(arabic); metadata(paymentSheet, arabic, actor);
            row(paymentSheet, 2, header, arabic ? new String[]{"رقم الدفعة","التاريخ","المورد","الفاتورة","المبلغ","العملة","الطريقة","الحالة"}
                    : new String[]{"Payment","Date","Supplier","Invoice","Amount","Currency","Method","Status"});
            index = 3;
            for (var value : payments) {
                var row = paymentSheet.createRow(index++);
                text(row, 0, value.paymentNumber()); date(row, 1, value.paymentDate()); text(row, 2, value.supplierName());
                text(row, 3, value.supplierInvoiceId()); number(row, 4, value.amount()); text(row, 5, value.currencyCode());
                text(row, 6, value.paymentMethod()); text(row, 7, value.status());
            }
            finish(paymentSheet, 8);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to export procurement workbook.", exception);
        }
    }

    private void metadata(org.apache.poi.ss.usermodel.Sheet sheet, boolean arabic, String actor) {
        var generated = sheet.createRow(0);
        text(generated, 0, (arabic ? "تاريخ الإنشاء: " : "Generated: ") + Instant.now());
        text(generated, 2, (arabic ? "المستخدم: " : "User: ") + actor);
    }
    private void row(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, CellStyle style, String[] values) {
        var row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) { var cell = row.createCell(i); cell.setCellValue(values[i]); cell.setCellStyle(style); }
    }
    private void text(org.apache.poi.ss.usermodel.Row row, int column, String value) { row.createCell(column).setCellValue(value == null ? "" : value); }
    private void number(org.apache.poi.ss.usermodel.Row row, int column, java.math.BigDecimal value) { if (value == null) text(row, column, ""); else row.createCell(column).setCellValue(value.doubleValue()); }
    private void date(org.apache.poi.ss.usermodel.Row row, int column, long epochMs) { row.createCell(column).setCellValue(java.util.Date.from(Instant.ofEpochMilli(epochMs))); }
    private void finish(org.apache.poi.ss.usermodel.Sheet sheet, int columns) { sheet.createFreezePane(0, 3); sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(2, sheet.getLastRowNum()), 0, columns - 1)); for (int i = 0; i < columns; i++) sheet.autoSizeColumn(i); }
}
