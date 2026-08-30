package com.bemo.hr.reportbuilder.domain;

import java.util.List;
import java.util.Map;

public record ReportDataset(
    String code,
    String labelAr,
    String labelEn,
    int version,
    List<FieldDef> fields,
    List<String> defaultSort,
    int maxLimit
) {
    public record FieldDef(String name, String labelAr, String labelEn,
                           String type, boolean dimension, String measureAggregate) {}

    public static final Map<String, ReportDataset> REGISTRY = Map.of(
        "sales_lines", new ReportDataset(
            "sales_lines", "_lin", "Lines", 1,
            List.of(
                new FieldDef("branchName", " الفرع", "Branch", "STRING", true, null),
                new FieldDef("month", "الشهر", "Month", "STRING", true, null),
                new FieldDef("customerName", "العميل", "Customer", "STRING", true, null),
                new FieldDef("itemName", "الصنف", "Item", "STRING", true, null),
                new FieldDef("quantity", "الكمية", "Quantity", "NUMERIC", false, "SUM"),
                new FieldDef("unitPrice", "سعر الوحدة", "Unit Price", "NUMERIC", false, "AVG"),
                new FieldDef("net", "الصافي", "Net Amount", "NUMERIC", false, "SUM"),
                new FieldDef("taxAmount", "الضريبة", "Tax Amount", "NUMERIC", false, "SUM"),
                new FieldDef("gross", "الإجمالي", "Gross Amount", "NUMERIC", false, "SUM")
            ),
            List.of("branchName", "month"), 10000
        ),
        "attendance_days", new ReportDataset(
            "attendance_days", "Days", "Days", 1,
            List.of(
                new FieldDef("employeeName", "الموظف", "Employee", "STRING", true, null),
                new FieldDef("categoryName", "الفئة", "Category", "STRING", true, null),
                new FieldDef("branchName", "الفرع", "Branch", "STRING", true, null),
                new FieldDef("date", "التاريخ", "Date", "DATE", true, null),
                new FieldDef("status", "الحالة", "Status", "STRING", true, null),
                new FieldDef("workedMinutes", "دقائق العمل", "Worked Minutes", "NUMERIC", false, "SUM"),
                new FieldDef("expectedMinutes", "الدقائق المتوقعة", "Expected Minutes", "NUMERIC", false, "SUM"),
                new FieldDef("latenessMinutes", "دقائق التأخر", "Lateness", "NUMERIC", false, "SUM"),
                new FieldDef("overtimeMinutes", "الإضافي", "Overtime", "NUMERIC", false, "SUM")
            ),
            List.of("employeeName", "date"), 10000
        ),
        "journal_lines", new ReportDataset(
            "journal_lines", " Journal", "Lines", 1,
            List.of(
                new FieldDef("accountCode", "رمز الحساب", "Account Code", "STRING", true, null),
                new FieldDef("accountName", "اسم الحساب", "Account Name", "STRING", true, null),
                new FieldDef("costCenter", "مركز التكلفة", "Cost Center", "STRING", true, null),
                new FieldDef("debit", "مدين", "Debit", "NUMERIC", false, "SUM"),
                new FieldDef("credit", "دائن", "Credit", "NUMERIC", false, "SUM"),
                new FieldDef("description", "الوصف", "Description", "STRING", true, null),
                new FieldDef("postingDate", "تاريخ القيود", "Posting Date", "DATE", true, null)
            ),
            List.of("accountCode", "postingDate"), 10000
        ),
        "stock_movements", new ReportDataset(
            "stock_movements", "Stock", "Movements", 1,
            List.of(
                new FieldDef("itemName", "الصنف", "Item", "STRING", true, null),
                new FieldDef("warehouse", "المخزن", "Warehouse", "STRING", true, null),
                new FieldDef("operationType", "نوع العملية", "Operation Type", "STRING", true, null),
                new FieldDef("quantity", "الكمية", "Quantity", "NUMERIC", false, "SUM"),
                new FieldDef("unitCost", "تكلفة الوحدة", "Unit Cost", "NUMERIC", false, "AVG"),
                new FieldDef("totalCost", "التكلفة الإجمالية", "Total Cost", "NUMERIC", false, "SUM"),
                new FieldDef("referenceCode", "المرجع", "Reference", "STRING", true, null),
                new FieldDef("movementDate", "التاريخ", "Date", "DATE", true, null)
            ),
            List.of("itemName", "movementDate"), 10000
        )
    );

    public static ReportDataset resolve(String code) {
        ReportDataset ds = REGISTRY.get(code);
        if (ds == null) throw new com.bemo.hr.shared.domain.BusinessRuleException(
            "Unknown dataset: " + code, "RB_FIELD_UNKNOWN", org.springframework.http.HttpStatus.BAD_REQUEST);
        return ds;
    }
}
