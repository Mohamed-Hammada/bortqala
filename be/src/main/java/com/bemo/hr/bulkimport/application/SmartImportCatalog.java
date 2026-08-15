package com.bemo.hr.bulkimport.application;

import com.bemo.hr.bulkimport.domain.SmartImportModels.Column;
import com.bemo.hr.bulkimport.domain.SmartImportModels.ColumnType;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Sheet;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Workflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SmartImportCatalog {
    private final Map<String, Workflow> workflows;

    public SmartImportCatalog() {
        var map = new LinkedHashMap<String, Workflow>();
        add(map, employees());
        add(map, accounts());
        add(map, journals());
        add(map, banks());
        add(map, budgets());
        add(map, parties());
        add(map, items());
        add(map, stockCount());
        add(map, payroll());
        add(map, bom());
        add(map, shifts());
        workflows = Map.copyOf(map);
    }

    private void add(Map<String, Workflow> target, Workflow workflow) { target.put(workflow.key(), workflow); }

    public List<Workflow> list() { return new ArrayList<>(workflows.values()); }

    public Workflow require(String key) {
        var workflow = workflows.get(key == null ? "" : key.toLowerCase(Locale.ROOT));
        if (workflow == null) throw new IllegalArgumentException("Unknown smart import workflow: " + key);
        return workflow;
    }

    public Column findColumn(Sheet sheet, String incomingHeader) {
        String normalized = normalize(incomingHeader);
        for (var column : sheet.columns()) {
            if (normalize(column.key()).equals(normalized)
                    || normalize(column.headerEn()).equals(normalized)
                    || normalize(column.headerAr()).equals(normalized)
                    || column.aliases().stream().map(this::normalize).anyMatch(normalized::equals)) {
                return column;
            }
        }
        return null;
    }

    public Sheet findSheet(Workflow workflow, String name) {
        String normalized = normalize(name);
        return workflow.sheets().stream()
                .filter(sheet -> normalize(sheet.key()).equals(normalized)
                        || normalize(sheet.titleEn()).equals(normalized)
                        || normalize(sheet.titleAr()).equals(normalized))
                .findFirst().orElse(null);
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.strip().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private static Column s(String key, String en, String ar, boolean required, String... aliases) {
        return new Column(key, en, ar, ColumnType.STRING, required, List.of(), List.of(aliases));
    }
    private static Column date(String key, String en, String ar, boolean required, String... aliases) {
        return new Column(key, en, ar, ColumnType.DATE, required, List.of(), List.of(aliases));
    }
    private static Column dec(String key, String en, String ar, boolean required, String... aliases) {
        return new Column(key, en, ar, ColumnType.DECIMAL, required, List.of(), List.of(aliases));
    }
    private static Column integer(String key, String en, String ar, boolean required, String... aliases) {
        return new Column(key, en, ar, ColumnType.INTEGER, required, List.of(), List.of(aliases));
    }
    private static Column bool(String key, String en, String ar, boolean required, String... aliases) {
        return new Column(key, en, ar, ColumnType.BOOLEAN, required, List.of("TRUE", "FALSE"), List.of(aliases));
    }
    private static Column enm(String key, String en, String ar, boolean required, List<String> values, String... aliases) {
        return new Column(key, en, ar, ColumnType.ENUM, required, values, List.of(aliases));
    }

    private static Workflow employees() {
        var personal = new Sheet("personal", "Personal & Contact Details", "البيانات الشخصية وبيانات الاتصال", List.of(
                s("EmployeeCode", "Employee Code", "كود الموظف", true, "كود", "employee_code"),
                s("NationalId", "National ID / Passport Number", "الرقم القومي / رقم جواز السفر", true, "PassportNumber", "الرقم القومي"),
                s("FirstNameAr", "First Name Arabic", "الاسم الأول بالعربية", false),
                s("LastNameAr", "Last Name Arabic", "اسم العائلة بالعربية", false),
                s("FirstNameEn", "First Name English", "الاسم الأول بالإنجليزية", false),
                s("LastNameEn", "Last Name English", "اسم العائلة بالإنجليزية", false),
                enm("Gender", "Gender", "النوع", false, List.of("MALE", "FEMALE")),
                date("DateOfBirth", "Date of Birth", "تاريخ الميلاد", false),
                s("MobilePhone", "Mobile Phone", "رقم الموبايل", false),
                s("PersonalEmail", "Personal Email", "البريد الشخصي", false),
                s("DeviceUserId", "Biometric Device User ID", "معرف الموظف على جهاز البصمة", false, "DeviceId")
        ));
        var employment = new Sheet("employment", "Employment & Department Placement", "التوظيف والتوزيع الإداري", List.of(
                s("EmployeeCode", "Employee Code", "كود الموظف", true),
                s("DepartmentCode", "Department Code / Name", "كود / اسم الإدارة", false, "DepartmentName"),
                s("BranchCode", "Branch Code / Name", "كود / اسم الفرع", false, "BranchName"),
                s("JobTitle", "Job Title", "المسمى الوظيفي", false),
                date("HireDate", "Hire Date", "تاريخ التعيين", true),
                enm("ContractType", "Contract Type", "نوع العقد", true, List.of("FULL_TIME", "PART_TIME", "TEMPORARY", "DAILY")),
                s("AssignedAttendanceCategory", "Attendance Category Code / Name", "كود / اسم فئة الحضور", true, "CategoryCode", "CategoryName")
        ));
        var salary = new Sheet("salary", "Salary & Banking", "الراتب والبيانات البنكية", List.of(
                s("EmployeeCode", "Employee Code", "كود الموظف", true),
                dec("BaseSalary", "Base Salary", "الراتب الأساسي", false),
                dec("HousingAllowance", "Housing Allowance", "بدل السكن", false),
                dec("TransportAllowance", "Transport Allowance", "بدل الانتقالات", false),
                dec("OtherFixedAllowance", "Other Fixed Allowance", "بدلات ثابتة أخرى", false),
                s("SocialInsuranceNumber", "Social Insurance Number", "رقم التأمين الاجتماعي", false),
                dec("SocialInsuranceSalaryCap", "Social Insurance Salary Cap", "أجر التأمين", false),
                enm("PaymentMethod", "Payment Method", "طريقة الدفع", false, List.of("BANK_TRANSFER", "CASH", "WALLET", "INSTAPAY")),
                s("BankName", "Bank Name", "اسم البنك", false),
                s("BankIBAN", "Bank IBAN", "رقم IBAN", false),
                s("MobileWalletNumber", "Mobile Wallet Number", "رقم المحفظة", false)
        ));
        return new Workflow("employees", "Bulk Employee Onboarding Wizard", "معالج إضافة الموظفين بالجملة",
                "/employees", "Employee_Master_Onboarding_Template.xlsx", "P0", true, List.of(personal, employment, salary));
    }

    private static Workflow accounts() {
        return new Workflow("accounts", "Chart of Accounts & Opening Balances Setup Wizard", "معالج دليل الحسابات والأرصدة الافتتاحية",
                "/finance/accounts", "Chart_of_Accounts_and_Opening_Balances.xlsx", "P1", false, List.of(new Sheet("accounts", "Accounts", "الحسابات", List.of(
                s("AccountCode", "Account Code", "كود الحساب", true), s("AccountNameAr", "Account Name Arabic", "اسم الحساب بالعربية", true),
                s("AccountNameEn", "Account Name English", "اسم الحساب بالإنجليزية", true), s("ParentAccountCode", "Parent Account Code", "كود الحساب الأب", false),
                enm("AccountType", "Account Type", "نوع الحساب", true, List.of("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE")),
                bool("IsPostingAllowed", "Posting Allowed", "السماح بالترحيل", true),
                enm("Currency", "Currency", "العملة", true, List.of("EGP", "USD", "EUR", "SAR", "AED")),
                dec("DebitOpeningBalance", "Debit Opening Balance", "الرصيد الافتتاحي المدين", false),
                dec("CreditOpeningBalance", "Credit Opening Balance", "الرصيد الافتتاحي الدائن", false),
                bool("CostCenterRequired", "Cost Center Required", "مركز التكلفة مطلوب", false)
        ))));
    }

    private static Workflow journals() {
        return new Workflow("journal-entries", "Batch Journal Voucher Ingestion Wizard", "معالج استيراد قيود اليومية المجمعة",
                "/finance/journal-entries", "Batch_Journal_Entries_Template.xlsx", "P1", false, List.of(new Sheet("journals", "Journal Entries", "قيود اليومية", List.of(
                s("JournalBatchRef", "Journal Batch Reference", "مرجع دفعة القيد", true), date("PostingDate", "Posting Date", "تاريخ الترحيل", true),
                enm("VoucherType", "Voucher Type", "نوع القيد", true, List.of("GENERAL", "ACCRUAL", "DEPRECIATION", "ADJUSTMENT")),
                integer("LineNumber", "Line Number", "رقم السطر", true), s("AccountCode", "Account Code", "كود الحساب", true),
                s("CostCenterCode", "Cost Center Code", "كود مركز التكلفة", false), dec("DebitAmount", "Debit Amount", "مدين", false),
                dec("CreditAmount", "Credit Amount", "دائن", false), s("Currency", "Currency", "العملة", false), dec("ExchangeRate", "Exchange Rate", "سعر الصرف", false),
                s("LineDescription", "Line Description", "وصف السطر", false), s("ExternalDocumentRef", "External Document Reference", "مرجع المستند الخارجي", false)
        ))));
    }

    private static Workflow banks() {
        return new Workflow("bank-statements", "Bank Statement Ingestion Wizard", "معالج استيراد كشف الحساب البنكي",
                "/finance/banks", "Bank_Statement_Standard_Template.xlsx", "P1", false, List.of(new Sheet("statement", "Bank Statement", "كشف البنك", List.of(
                date("StatementDate", "Statement Date", "تاريخ الكشف", true), date("ValueDate", "Value Date", "تاريخ القيمة", true),
                s("BankTransactionRef", "Bank Transaction Reference", "مرجع حركة البنك", true), s("TransactionDescription", "Transaction Description", "وصف الحركة", false),
                dec("DebitAmount", "Debit Amount", "مدين", false), dec("CreditAmount", "Credit Amount", "دائن", false),
                dec("BalanceAfter", "Balance After", "الرصيد بعد الحركة", false), s("ChequeNumber", "Cheque Number", "رقم الشيك", false)
        ))));
    }

    private static Workflow budgets() {
        var columns = new ArrayList<Column>();
        columns.add(integer("FiscalYear", "Fiscal Year", "السنة المالية", true));
        columns.add(s("BudgetVersion", "Budget Version", "إصدار الموازنة", true));
        columns.add(s("DepartmentCode", "Department / Cost Center Code", "كود الإدارة / مركز التكلفة", true, "CostCenterCode"));
        columns.add(s("AccountCode", "Account Code", "كود الحساب", true));
        columns.add(s("LineDescription", "Line Description", "وصف البند", false));
        for (String month : List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")) {
            columns.add(dec(month + "_Amount", month + " Amount", "قيمة " + month, false));
        }
        columns.add(dec("TotalAnnualBudget", "Total Annual Budget", "إجمالي الموازنة السنوية", true));
        return new Workflow("budgets", "Departmental Budget Upload Wizard", "معالج رفع موازنات الإدارات",
                "/finance/budgets", "Annual_Budget_Matrix_Template.xlsx", "P2", false, List.of(new Sheet("budget", "Annual Budget", "الموازنة السنوية", columns)));
    }

    private static Workflow parties() {
        return new Workflow("parties", "Customer & Supplier Master Import Wizard", "معالج استيراد العملاء والموردين",
                "/parties", "Business_Party_Directory_Template.xlsx", "P2", false, List.of(new Sheet("parties", "Business Parties", "الأطراف التجارية", List.of(
                enm("PartyType", "Party Type", "نوع الطرف", true, List.of("CUSTOMER", "SUPPLIER", "CONTRACTOR", "BOTH")),
                s("PartyCode", "Party Code", "كود الطرف", true), s("PartyNameAr", "Party Name Arabic", "اسم الطرف بالعربية", true), s("PartyNameEn", "Party Name English", "اسم الطرف بالإنجليزية", false),
                s("TaxRegistrationNumber", "Tax Registration Number", "رقم التسجيل الضريبي", false), s("CommercialRegistrationNumber", "Commercial Registration Number", "رقم السجل التجاري", false),
                dec("CreditLimitAmount", "Credit Limit", "حد الائتمان", false), integer("PaymentTermsDays", "Payment Terms Days", "أيام السداد", false),
                s("PrimaryContactName", "Primary Contact", "جهة الاتصال الرئيسية", false), s("Phone", "Phone", "الهاتف", false), s("Mobile", "Mobile", "الموبايل", false), s("Email", "Email", "البريد الإلكتروني", false),
                s("BillingAddress", "Billing Address", "عنوان الفواتير", false), s("City", "City", "المدينة", false), s("Country", "Country", "الدولة", false),
                s("DefaultCurrency", "Default Currency", "العملة الافتراضية", false), s("BankName", "Bank Name", "اسم البنك", false), s("BankBranch", "Bank Branch", "فرع البنك", false), s("BankIBAN", "Bank IBAN", "IBAN", false)
        ))));
    }

    private static Workflow items() {
        return new Workflow("items", "Product Catalog & Initial Stock Wizard", "معالج كتالوج الأصناف والمخزون الافتتاحي",
                "/operations", "Item_Catalog_and_Opening_Stock.xlsx", "P2", false, List.of(new Sheet("items", "Items & Opening Stock", "الأصناف والمخزون الافتتاحي", List.of(
                s("ItemCode", "Item Code", "كود الصنف", true), s("Barcode", "Barcode", "الباركود", false), s("ItemNameAr", "Item Name Arabic", "اسم الصنف بالعربية", true), s("ItemNameEn", "Item Name English", "اسم الصنف بالإنجليزية", false),
                s("ItemCategoryCode", "Item Category Code / Name", "كود / اسم فئة الصنف", false), enm("PrimaryUoM", "Primary UoM", "وحدة القياس الأساسية", true, List.of("PCS", "KG", "BOX", "METER", "LITER")),
                dec("StandardCost", "Standard Cost", "التكلفة المعيارية", false), dec("DefaultSellingPrice", "Default Selling Price", "سعر البيع الافتراضي", false), dec("ReorderPointLevel", "Reorder Point", "حد إعادة الطلب", false),
                bool("IsLotTracked", "Lot Tracked", "تتبع التشغيلات", false), bool("IsSerialTracked", "Serial Tracked", "تتبع السيريال", false),
                s("OpeningWarehouseCode", "Opening Warehouse Code", "كود مخزن الرصيد الافتتاحي", false), s("OpeningBinCode", "Opening Bin Code", "كود الموقع", false),
                dec("OpeningQuantity", "Opening Quantity", "الكمية الافتتاحية", false), s("OpeningLotNumber", "Opening Lot Number", "رقم التشغيلة الافتتاحية", false), date("OpeningExpiryDate", "Opening Expiry Date", "تاريخ انتهاء التشغيلة", false)
        ))));
    }

    private static Workflow stockCount() {
        return new Workflow("stock-count", "Physical Stock Count Reconciliation Wizard", "معالج تسوية الجرد الفعلي",
                "/operations", "Physical_Stock_Count_Template.xlsx", "P2", false, List.of(new Sheet("count", "Stock Count", "الجرد الفعلي", List.of(
                s("WarehouseCode", "Warehouse Code", "كود المخزن", true), s("BinLocation", "Bin Location", "الموقع", false), s("ItemCode", "Item Code", "كود الصنف", true), s("ItemDescription", "Item Description", "وصف الصنف", false),
                s("LotNumber", "Lot Number", "رقم التشغيلة", false), dec("BookQuantity", "Book Quantity", "كمية الدفاتر", false), dec("CountedPhysicalQuantity", "Counted Physical Quantity", "الكمية الفعلية", true),
                s("CounterPersonName", "Counter Person Name", "اسم القائم بالجرد", false), s("DiscrepancyNotes", "Discrepancy Notes", "ملاحظات الفروقات", false)
        ))));
    }

    private static Workflow payroll() {
        return new Workflow("payroll-adjustments", "Monthly Variable Payroll Adjustments Wizard", "معالج المتغيرات الشهرية للرواتب",
                "/payroll", "Monthly_Payroll_Variable_Inputs.xlsx", "P0", false, List.of(new Sheet("payroll", "Payroll Variables", "متغيرات الرواتب", List.of(
                s("PayPeriodCode", "Pay Period Code", "كود فترة الرواتب", true), s("EmployeeCode", "Employee Code", "كود الموظف", true), s("EmployeeName", "Employee Name", "اسم الموظف", false),
                enm("ComponentCode", "Component Code", "كود المكون", true, List.of("OVERTIME_HOURS", "SALES_COMMISSION", "PERFORMANCE_BONUS", "ADVANCE_DEDUCTION", "DISCIPLINARY_PENALTY", "TRANSPORT_ADJUSTMENT")),
                dec("AmountOrQuantity", "Amount or Quantity", "القيمة أو الكمية", true), s("NotesReason", "Notes / Reason", "الملاحظات / السبب", false), s("SupervisorApprovalRef", "Supervisor Approval Reference", "مرجع اعتماد المشرف", false)
        ))));
    }

    private static Workflow bom() {
        return new Workflow("bom-routing", "Bill of Materials (BOM) & Routing Wizard", "معالج قوائم الخامات ومسارات التشغيل",
                "/manufacturing/production", "BOM_and_Routing_Setup_Template.xlsx", "P3", false, List.of(new Sheet("bom", "BOM & Routing", "BOM ومسار التشغيل", List.of(
                s("ParentFinishedItemCode", "Parent Finished Item Code", "كود الصنف التام", true), s("BOMVersion", "BOM Version", "إصدار BOM", true), s("ComponentRawItemCode", "Component Raw Item Code", "كود المكون الخام", true),
                dec("QuantityPerUnit", "Quantity Per Unit", "الكمية لكل وحدة", true), s("ComponentUoM", "Component UoM", "وحدة قياس المكون", true), dec("ScrapFactorPercent", "Scrap Factor Percent", "نسبة الهالك", false),
                integer("OperationSequenceNumber", "Operation Sequence Number", "ترتيب العملية", false), s("WorkCenterCode", "Work Center Code", "كود مركز العمل", false),
                dec("OperationSetupTimeMinutes", "Operation Setup Time Minutes", "وقت الإعداد بالدقائق", false), dec("OperationRunTimePerUnitMinutes", "Operation Run Time Per Unit Minutes", "وقت التشغيل للوحدة بالدقائق", false)
        ))));
    }

    private static Workflow shifts() {
        var columns = new ArrayList<Column>();
        columns.add(s("EmployeeCode", "Employee Code", "كود الموظف", true));
        columns.add(s("EmployeeName", "Employee Name", "اسم الموظف", false));
        for (int day = 1; day <= 31; day++) {
            String key = String.format("Day_%02d", day);
            columns.add(enm(key, key, "اليوم " + day, false, List.of("M", "E", "N", "OFF", "LEAVE")));
        }
        columns.add(s("DefaultLocationGateCode", "Default Location / Gate Code", "كود الموقع / البوابة", false));
        return new Workflow("shift-roster", "Monthly Shift Roster Planning Wizard", "معالج تخطيط ورديات الشهر",
                "/categories", "Monthly_Shift_Roster_Grid.xlsx", "P3", false, List.of(new Sheet("roster", "Monthly Roster", "جدول الورديات الشهري", columns)));
    }
}
