package com.bemo.hr.shared.dataexchange;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.bemo.hr.shared.dataexchange.SpreadsheetTemplateDefinition.ColumnType.BOOLEAN;
import static com.bemo.hr.shared.dataexchange.SpreadsheetTemplateDefinition.ColumnType.DATE;
import static com.bemo.hr.shared.dataexchange.SpreadsheetTemplateDefinition.ColumnType.DATETIME;
import static com.bemo.hr.shared.dataexchange.SpreadsheetTemplateDefinition.ColumnType.DECIMAL;
import static com.bemo.hr.shared.dataexchange.SpreadsheetTemplateDefinition.ColumnType.INTEGER;
import static com.bemo.hr.shared.dataexchange.SpreadsheetTemplateDefinition.ColumnType.TEXT;

@Component
public class SpreadsheetTemplateCatalog {
    private final Map<String, SpreadsheetTemplateDefinition> definitions;

    public SpreadsheetTemplateCatalog() {
        this.definitions = buildDefinitions();
    }

    public List<SpreadsheetTemplateDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public SpreadsheetTemplateDefinition require(String key) {
        SpreadsheetTemplateDefinition definition = definitions.get(key);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown spreadsheet template: " + key);
        }
        return definition;
    }

    private Map<String, SpreadsheetTemplateDefinition> buildDefinitions() {
        Map<String, SpreadsheetTemplateDefinition> result = new LinkedHashMap<>();

        add(result, single("contractor-rate-adjustments", "workforce", "Manual contractor rate adjustments",
                "/workforce/settlement-periods",
                "Bulk approved contractor rate/bonus adjustments for a settlement period.",
                cols(
                        req("ContractorCode", TEXT, true, "CONT-001", "Contractor code"),
                        req("Period", TEXT, false, "2026-08", "Settlement period"),
                        req("AdjustmentType", TEXT, false, "RATE", "RATE or BONUS", List.of("RATE", "BONUS")),
                        req("Amount", DECIMAL, false, "250.00", "Adjustment amount"),
                        opt("Reason", TEXT, "Approved site premium", "Business justification"),
                        opt("ApprovedReference", TEXT, "APR-2026-0815", "Approval reference"))));

        add(result, single("bulk-labor-request", "workforce", "Bulk labor request",
                "/workforce/labor-requests",
                "Weekly labor requisitions from site/project managers.",
                cols(
                        req("Date", DATE, false, "2026-08-17", "Requested work date"),
                        req("Site", TEXT, false, "SITE-01", "Site/project code"),
                        req("Category", TEXT, false, "ELECTRICIAN", "Worker category"),
                        req("Headcount", INTEGER, false, "12", "Required headcount"),
                        req("Shift", TEXT, false, "DAY", "Shift code"),
                        opt("Notes", TEXT, "North zone", "Request notes"))));

        add(result, single("bulk-cash-advance", "workforce", "Bulk cash advance issuance",
                "/workforce/advances",
                "Bulk worker advance distribution list.",
                cols(
                        req("WorkerCode", TEXT, true, "WRK-1001", "Worker code"),
                        req("NationalID", TEXT, false, "29801011234567", "National ID"),
                        req("AdvanceAmount", DECIMAL, false, "3000.00", "Advance amount"),
                        req("InstallmentCount", INTEGER, false, "3", "Installment count"),
                        req("DisbursementDate", DATE, false, "2026-08-15", "Disbursement date"),
                        opt("Reference", TEXT, "SITE-CASH-55", "External reference"))));

        add(result, single("worker-master", "workforce", "Worker master data",
                "/workforce/workers",
                "Worker master onboarding with duplicate-sensitive identity fields.",
                cols(
                        req("WorkerCode", TEXT, true, "WRK-1001", "Unique worker code"),
                        req("FullName", TEXT, false, "Ahmed Hassan", "Worker full name"),
                        req("NationalID", TEXT, true, "29801011234567", "Unique national ID"),
                        req("Category", TEXT, false, "SKILLED", "Worker category"),
                        opt("Nationality", TEXT, "EG", "Nationality code"),
                        opt("Gender", TEXT, "MALE", "Gender", List.of("MALE", "FEMALE")),
                        opt("ContractorCode", TEXT, "CONT-001", "Contractor agency"),
                        opt("MobileWallet", TEXT, "01001234567", "Mobile wallet identifier"),
                        opt("Active", BOOLEAN, "true", "Active status"))));

        add(result, single("contractor-onboarding", "workforce", "Contractor onboarding",
                "/workforce/contractors",
                "Bulk contractor profiles, compliance identity and payment terms.",
                cols(
                        req("ContractorCode", TEXT, true, "CONT-001", "Unique contractor code"),
                        req("ContractorName", TEXT, false, "Delta Manpower", "Contractor name"),
                        opt("CommercialRegistration", TEXT, "CR-99218", "Commercial registration"),
                        opt("TaxNumber", TEXT, "TAX-3321", "Tax registration"),
                        opt("PaymentTermsDays", INTEGER, "30", "Payment terms in days"),
                        opt("IBAN", TEXT, "EG380019000500000000263180002", "Settlement IBAN"),
                        opt("Phone", TEXT, "01000000000", "Contact phone"),
                        opt("Active", BOOLEAN, "true", "Active status"))));

        add(result, single("manual-punch-log", "attendance", "Manual punch log",
                "/reports",
                "Manual attendance punches for remote sites without networked devices.",
                cols(
                        req("EmployeeCode", TEXT, false, "EMP-1001", "Employee code"),
                        req("PunchDateTime", DATETIME, false, "2026-08-15T07:58:00", "Punch timestamp"),
                        req("Direction", TEXT, false, "IN", "IN or OUT", List.of("IN", "OUT")),
                        req("LocationCode", TEXT, false, "SITE-01", "Location code"),
                        opt("DeviceReference", TEXT, "MANUAL-SHEET", "Source/device reference"))));

        add(result, single("zkteco-punch-log", "attendance", "ZKTeco punch normalization",
                "/imports/device-integrations",
                "Normalized staging format for ZKTeco .dat/.csv exports.",
                cols(
                        req("EmployeeCode", TEXT, false, "1001", "Employee/device user code"),
                        req("PunchDateTime", DATETIME, false, "2026-08-15T07:58:00", "Punch timestamp"),
                        opt("VerifyMode", TEXT, "Fingerprint", "Verification mode"),
                        opt("DeviceId", TEXT, "ZK-GATE-1", "Terminal identifier"))));

        add(result, single("hikvision-punch-log", "attendance", "Hikvision punch normalization",
                "/imports/device-integrations",
                "Normalized staging format for Hikvision attendance CSV exports.",
                cols(
                        req("EmployeeCode", TEXT, false, "1001", "Employee number"),
                        req("PunchDateTime", DATETIME, false, "2026-08-15T07:58:00", "Event timestamp"),
                        opt("Direction", TEXT, "IN", "IN or OUT", List.of("IN", "OUT")),
                        opt("DeviceId", TEXT, "HIK-GATE-1", "Terminal identifier"))));

        add(result, single("suprema-punch-log", "attendance", "Suprema BioStar punch normalization",
                "/imports/device-integrations",
                "Normalized staging format for Suprema BioStar exports.",
                cols(
                        req("EmployeeCode", TEXT, false, "1001", "User ID"),
                        req("PunchDateTime", DATETIME, false, "2026-08-15T07:58:00", "Event timestamp"),
                        opt("EventType", TEXT, "ENTRY", "BioStar event type"),
                        opt("DeviceId", TEXT, "SUP-GATE-1", "Terminal identifier"))));

        add(result, employeeOnboarding());

        add(result, single("shift-assignment", "employees", "Shift assignment",
                "/categories",
                "Bulk employee assignment to shift/category schedules.",
                cols(
                        req("EmployeeCode", TEXT, false, "EMP-1001", "Employee code"),
                        req("ShiftCode", TEXT, false, "DAY-A", "Shift code"),
                        req("EffectiveFrom", DATE, false, "2026-08-01", "Effective start date"),
                        opt("EffectiveTo", DATE, "2026-08-31", "Optional end date"),
                        opt("ProjectCode", TEXT, "PRJ-01", "Optional project roster"))));

        add(result, single("payroll-variables", "payroll", "Variable earnings & deductions",
                "/payroll",
                "Monthly variable payroll inputs before payroll calculation/freeze.",
                cols(
                        req("EmployeeCode", TEXT, false, "EMP-1001", "Employee code"),
                        req("PayrollPeriod", TEXT, false, "2026-08", "Payroll period"),
                        req("Type", TEXT, false, "BONUS", "BONUS, INCENTIVE, COMMISSION or DEDUCTION",
                                List.of("BONUS", "INCENTIVE", "COMMISSION", "DEDUCTION")),
                        req("Amount", DECIMAL, false, "500.00", "Variable amount"),
                        opt("Reference", TEXT, "BONUS-0815", "Approval/reference"),
                        opt("Notes", TEXT, "Site completion bonus", "Notes"))));

        add(result, single("chart-of-accounts", "finance", "Chart of accounts initialization",
                "/finance/accounts",
                "Seed account hierarchy for a new entity/company.",
                cols(
                        req("AccountCode", TEXT, true, "110100", "Unique account code"),
                        req("AccountName", TEXT, false, "Cash on Hand", "Account name"),
                        req("AccountType", TEXT, false, "ASSET", "Account type",
                                List.of("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE")),
                        opt("ParentAccountCode", TEXT, "110000", "Parent account code"),
                        opt("PostingAllowed", BOOLEAN, "true", "Posting allowed"),
                        opt("Active", BOOLEAN, "true", "Active status"))));

        add(result, single("journal-entry", "finance", "Manual journal entry",
                "/finance/journal-entries",
                "Multi-line journal voucher staging format. Balance validation is performed by the finance command path.",
                cols(
                        req("VoucherReference", TEXT, false, "JV-2026-0815", "Groups voucher lines"),
                        req("AccountCode", TEXT, false, "610100", "GL account code"),
                        opt("CostCenter", TEXT, "CC-OPS", "Cost center"),
                        req("Debit", DECIMAL, false, "1000.00", "Debit amount; use zero when crediting"),
                        req("Credit", DECIMAL, false, "0.00", "Credit amount; use zero when debiting"),
                        opt("LineDescription", TEXT, "Monthly accrual", "Line description"),
                        opt("ReferenceDoc", TEXT, "INV-991", "Supporting document reference"),
                        opt("PostingDate", DATE, "2026-08-15", "Posting date"))));

        add(result, single("bank-statement", "finance", "Bank statement",
                "/finance/banks",
                "Standard bank statement format for reconciliation.",
                cols(
                        req("Date", DATE, false, "2026-08-15", "Transaction date"),
                        opt("ValueDate", DATE, "2026-08-15", "Value date"),
                        opt("Reference", TEXT, "BANK-TRX-991", "Bank reference"),
                        req("Description", TEXT, false, "Supplier transfer", "Narration"),
                        req("Debit", DECIMAL, false, "1200.00", "Debit"),
                        req("Credit", DECIMAL, false, "0.00", "Credit"),
                        opt("Balance", DECIMAL, "50000.00", "Running bank balance"))));

        add(result, single("annual-budget", "finance", "Annual budget upload",
                "/finance/budgets",
                "Department/cost-center budget lines across fiscal periods.",
                cols(
                        req("FiscalYear", INTEGER, false, "2027", "Fiscal year"),
                        req("DepartmentCode", TEXT, false, "OPS", "Department code"),
                        req("AccountCode", TEXT, false, "620100", "Expense/account code"),
                        req("Period", TEXT, false, "2027-01", "Fiscal period"),
                        req("BudgetAmount", DECIMAL, false, "250000.00", "Budget amount"),
                        opt("Notes", TEXT, "Approved annual plan", "Notes"))));

        add(result, single("purchase-requisition", "procurement", "Purchase requisition bulk upload",
                "/trade/procurement",
                "Bulk multi-item project material requests.",
                cols(
                        req("RequestReference", TEXT, false, "PR-0815-01", "Groups request lines"),
                        req("ItemCode", TEXT, false, "RM-100", "Item code"),
                        req("Quantity", DECIMAL, false, "50", "Requested quantity"),
                        opt("Uom", TEXT, "KG", "Unit of measure"),
                        opt("RequiredDate", DATE, "2026-08-20", "Required-by date"),
                        opt("WarehouseCode", TEXT, "WH-01", "Destination warehouse"),
                        opt("ProjectCode", TEXT, "PRJ-01", "Project/cost object"),
                        opt("Notes", TEXT, "Production requirement", "Notes"))));

        add(result, single("supplier-price-catalog", "procurement", "Supplier price catalog",
                "/trade/procurement",
                "Supplier SKU catalog and effective prices.",
                cols(
                        req("SupplierCode", TEXT, false, "SUP-001", "Supplier code"),
                        req("ItemCode", TEXT, false, "RM-100", "Item/SKU code"),
                        req("UnitPrice", DECIMAL, false, "125.50", "Unit price"),
                        opt("Currency", TEXT, "EGP", "Currency"),
                        opt("MinimumQuantity", DECIMAL, "10", "Minimum order quantity"),
                        opt("EffectiveFrom", DATE, "2026-08-15", "Effective start date"),
                        opt("EffectiveTo", DATE, "2026-12-31", "Optional end date"))));

        add(result, single("sales-order", "sales", "Bulk sales order",
                "/trade/sales",
                "High-volume sales order line import.",
                cols(
                        req("OrderReference", TEXT, false, "SO-BULK-001", "Groups order lines"),
                        req("CustomerCode", TEXT, false, "CUS-001", "Customer code"),
                        req("ItemCode", TEXT, false, "FG-100", "Item code"),
                        req("Quantity", DECIMAL, false, "20", "Ordered quantity"),
                        opt("UnitPrice", DECIMAL, "250.00", "Optional agreed unit price"),
                        opt("WarehouseCode", TEXT, "WH-01", "Fulfilment warehouse"),
                        opt("RequiredDate", DATE, "2026-08-20", "Required delivery date"),
                        opt("CustomerReference", TEXT, "PO-CUST-900", "Customer PO/reference"))));

        add(result, single("customer-price-matrix", "sales", "Customer price matrix",
                "/trade/sales",
                "Tiered customer/group pricing and discount agreements.",
                cols(
                        req("CustomerOrGroupCode", TEXT, false, "CUS-GOLD", "Customer or customer group code"),
                        req("ItemCode", TEXT, false, "FG-100", "Item code"),
                        opt("Price", DECIMAL, "240.00", "Agreed price"),
                        opt("DiscountPercent", DECIMAL, "5.0", "Discount percentage"),
                        opt("MinimumQuantity", DECIMAL, "10", "Quantity break"),
                        opt("EffectiveFrom", DATE, "2026-08-15", "Effective start date"),
                        opt("EffectiveTo", DATE, "2026-12-31", "Optional end date"))));

        add(result, single("physical-count", "inventory", "Physical count reconciliation",
                "/operations",
                "Completed stock take counts to generate controlled stock adjustments through the inventory command path.",
                cols(
                        req("Warehouse", TEXT, false, "WH-01", "Warehouse code"),
                        opt("Bin", TEXT, "A-01-01", "Bin location"),
                        req("ItemCode", TEXT, false, "RM-100", "Item code"),
                        opt("LotNumber", TEXT, "LOT-001", "Lot/batch number"),
                        req("PhysicalQuantity", DECIMAL, false, "48", "Counted quantity"),
                        opt("CountedBy", TEXT, "EMP-1001", "Counter employee code"),
                        opt("CountDate", DATE, "2026-08-15", "Count date"))));

        add(result, single("opening-stock", "inventory", "Initial stock opening balances",
                "/operations",
                "Warehouse go-live stock balances.",
                cols(
                        req("ItemCode", TEXT, false, "RM-100", "Item code"),
                        req("Warehouse", TEXT, false, "WH-01", "Warehouse code"),
                        opt("Bin", TEXT, "A-01-01", "Bin location"),
                        req("Quantity", DECIMAL, false, "100", "Opening quantity"),
                        req("UnitCost", DECIMAL, false, "12.50", "Opening unit cost"),
                        opt("LotNumber", TEXT, "LOT-001", "Lot/batch number"),
                        opt("ExpiryDate", DATE, "2027-08-15", "Expiry date"))));

        add(result, single("stock-transfer", "inventory", "Stock transfer order",
                "/operations",
                "Bulk warehouse-to-warehouse transfer requests.",
                cols(
                        req("TransferReference", TEXT, false, "TRF-001", "Groups transfer lines"),
                        req("FromWarehouse", TEXT, false, "WH-01", "Source warehouse"),
                        req("ToWarehouse", TEXT, false, "WH-02", "Destination warehouse"),
                        req("ItemCode", TEXT, false, "RM-100", "Item code"),
                        req("Quantity", DECIMAL, false, "25", "Transfer quantity"),
                        opt("LotNumber", TEXT, "LOT-001", "Lot/batch number"),
                        opt("RequestedDate", DATE, "2026-08-15", "Requested transfer date"))));

        add(result, single("bom", "manufacturing", "Bill of materials (BOM)",
                "/manufacturing/production",
                "Multi-level BOM component hierarchy.",
                cols(
                        req("ParentItemCode", TEXT, false, "FG-100", "Parent/finished item"),
                        req("ComponentItemCode", TEXT, false, "RM-100", "Component item"),
                        req("QuantityPerUnit", DECIMAL, false, "2.5", "Quantity per parent unit"),
                        opt("ScrapFactorPercent", DECIMAL, "2.0", "Expected scrap percentage"),
                        req("OperationSequence", INTEGER, false, "10", "Operation sequence"),
                        opt("EffectiveFrom", DATE, "2026-08-15", "Effective start date"))));

        add(result, single("party-master", "governance", "Customer / supplier master",
                "/parties",
                "Bulk customer/supplier onboarding.",
                cols(
                        req("PartyName", TEXT, false, "Acme Trading", "Party name"),
                        req("Type", TEXT, false, "CUSTOMER", "CUSTOMER or SUPPLIER", List.of("CUSTOMER", "SUPPLIER")),
                        opt("TaxNumber", TEXT, "TAX-001", "Tax number"),
                        opt("CommercialReg", TEXT, "CR-001", "Commercial registration"),
                        opt("Phone", TEXT, "01000000000", "Phone"),
                        opt("CreditLimit", DECIMAL, "100000.00", "Credit limit"),
                        opt("PaymentTerms", INTEGER, "30", "Payment terms days"),
                        opt("IBAN", TEXT, "EG380019000500000000263180002", "IBAN"))));

        add(result, single("translation-dictionary", "governance", "Dynamic translation re-import",
                "/settings/translations",
                "Translation agency exchange workbook. Existing translation-management import remains authoritative for persistence.",
                cols(
                        req("TranslationKey", TEXT, false, "payroll.title", "Translation key"),
                        req("Locale", TEXT, false, "ar-EG", "Locale", List.of("ar-EG", "en-US")),
                        req("TextValue", TEXT, false, "الرواتب", "Translated text"),
                        opt("AppCode", TEXT, "", "Optional application scope"))));

        return result;
    }

    private SpreadsheetTemplateDefinition employeeOnboarding() {
        List<SpreadsheetTemplateDefinition.SheetDefinition> sheets = new ArrayList<>();
        sheets.add(sheet("Personal Details", cols(
                req("EmployeeCode", TEXT, true, "EMP-1001", "Unique employee code"),
                req("Name", TEXT, false, "Sara Ali", "Full name"),
                req("NationalID", TEXT, true, "29801011234567", "National ID"),
                opt("DOB", DATE, "1998-01-01", "Date of birth"),
                opt("Gender", TEXT, "FEMALE", "Gender", List.of("MALE", "FEMALE")),
                opt("Contact", TEXT, "01000000000", "Contact phone"))));
        sheets.add(sheet("Employment Info", cols(
                req("EmployeeCode", TEXT, false, "EMP-1001", "Employee code"),
                req("HireDate", DATE, false, "2026-08-01", "Hire date"),
                req("Department", TEXT, false, "OPS", "Department code"),
                req("JobTitle", TEXT, false, "Supervisor", "Job title"),
                opt("ShiftCategory", TEXT, "DAY-A", "Shift/category"))));
        sheets.add(sheet("Salary & Compensation", cols(
                req("EmployeeCode", TEXT, false, "EMP-1001", "Employee code"),
                req("BasePay", DECIMAL, false, "15000.00", "Base pay"),
                opt("Allowances", DECIMAL, "1500.00", "Recurring allowances"),
                opt("SocialInsuranceNumber", TEXT, "SI-001", "Social insurance number"))));
        sheets.add(sheet("Bank & Payment Details", cols(
                req("EmployeeCode", TEXT, false, "EMP-1001", "Employee code"),
                opt("IBAN", TEXT, "EG380019000500000000263180002", "IBAN"),
                opt("BankName", TEXT, "NBE", "Bank name"),
                opt("MobileWallet", TEXT, "01000000000", "Mobile wallet"))));
        return new SpreadsheetTemplateDefinition(
                "employee-onboarding", "employees", "Bulk employee onboarding", "/employees",
                "Four-sheet employee onboarding workbook covering personal, employment, salary and payment data.",
                false, sheets);
    }

    private static SpreadsheetTemplateDefinition single(String key, String module, String title,
                                                          String route, String description,
                                                          List<SpreadsheetTemplateDefinition.ColumnDefinition> columns) {
        return new SpreadsheetTemplateDefinition(key, module, title, route, description, false,
                List.of(sheet("Data", columns)));
    }

    private static SpreadsheetTemplateDefinition.SheetDefinition sheet(
            String name, List<SpreadsheetTemplateDefinition.ColumnDefinition> columns) {
        Map<String, Object> sample = new LinkedHashMap<>();
        for (SpreadsheetTemplateDefinition.ColumnDefinition column : columns) {
            if (column.example() != null && !column.example().isBlank()) {
                sample.put(column.header(), typedExample(column));
            }
        }
        return new SpreadsheetTemplateDefinition.SheetDefinition(name, columns, List.of(sample));
    }

    private static Object typedExample(SpreadsheetTemplateDefinition.ColumnDefinition column) {
        String value = column.example();
        try {
            return switch (column.type()) {
                case INTEGER -> Integer.valueOf(value);
                case DECIMAL -> new BigDecimal(value);
                case DATE -> LocalDate.parse(value);
                case BOOLEAN -> Boolean.valueOf(value);
                default -> value;
            };
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private static List<SpreadsheetTemplateDefinition.ColumnDefinition> cols(
            SpreadsheetTemplateDefinition.ColumnDefinition... columns) {
        return List.of(columns);
    }

    private static SpreadsheetTemplateDefinition.ColumnDefinition req(String header,
                                                                        SpreadsheetTemplateDefinition.ColumnType type,
                                                                        boolean unique,
                                                                        String example,
                                                                        String description) {
        return new SpreadsheetTemplateDefinition.ColumnDefinition(header, header, true, type, unique,
                List.of(), example, description);
    }

    private static SpreadsheetTemplateDefinition.ColumnDefinition req(String header,
                                                                        SpreadsheetTemplateDefinition.ColumnType type,
                                                                        boolean unique,
                                                                        String example,
                                                                        String description,
                                                                        List<String> allowedValues) {
        return new SpreadsheetTemplateDefinition.ColumnDefinition(header, header, true, type, unique,
                allowedValues, example, description);
    }

    private static SpreadsheetTemplateDefinition.ColumnDefinition opt(String header,
                                                                        SpreadsheetTemplateDefinition.ColumnType type,
                                                                        String example,
                                                                        String description) {
        return new SpreadsheetTemplateDefinition.ColumnDefinition(header, header, false, type, false,
                List.of(), example, description);
    }

    private static SpreadsheetTemplateDefinition.ColumnDefinition opt(String header,
                                                                        SpreadsheetTemplateDefinition.ColumnType type,
                                                                        String example,
                                                                        String description,
                                                                        List<String> allowedValues) {
        return new SpreadsheetTemplateDefinition.ColumnDefinition(header, header, false, type, false,
                allowedValues, example, description);
    }

    private static void add(Map<String, SpreadsheetTemplateDefinition> result,
                            SpreadsheetTemplateDefinition definition) {
        result.put(definition.key(), definition);
    }
}
