package com.bemo.hr.shared.sampletemplate;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SampleTemplateCatalog {
    public record Column(String en, String ar, boolean required, String type, String accepted, String notes) {}
    public record Template(String fileName, List<Column> columns, List<List<Object>> samples) {}

    private static Column c(String en, String ar, boolean required, String type, String accepted, String notes) {
        return new Column(en, ar, required, type, accepted == null ? "" : accepted, notes == null ? "" : notes);
    }

    private final Map<String, Template> templates = Map.ofEntries(
        Map.entry("ATTENDANCE", new Template("biometric-attendance-sample.xlsx", List.of(
            c("Employee Code", "كود الموظف", true, "String / Alphanumeric", "", "Matches Employee ID / Enrollment PIN"),
            c("Timestamp", "تاريخ ووقت البصمة", true, "YYYY-MM-DD HH:mm:ss", "", "Local machine timestamp"),
            c("Punch Type", "نوع البصمة", false, "Enum", "IN, OUT, CHECK_IN, CHECK_OUT", "Auto-paired if omitted"),
            c("Device Serial", "رقم الجهاز", false, "String", "", "Hardware serial identifier"),
            c("Verification Mode", "طريقة التحقق", false, "Enum", "FINGERPRINT, FACE, CARD, PIN", "Verification biometric mode"),
            c("Work Code", "كود العمل", false, "Integer / String", "", "Default is 0")
        ), List.of(
            List.of("EMP-1001", "2026-08-15 08:30:00", "IN", "ZKT-01", "FINGERPRINT", 0),
            List.of("EMP-1001", "2026-08-15 17:05:00", "OUT", "ZKT-01", "FINGERPRINT", 0),
            List.of("EMP-1002", "2026-08-15 08:45:12", "IN", "ZKT-01", "FACE", 0)
        ))),
        Map.entry("EMPLOYEE_MASTER", new Template("employee-master-sample.xlsx", List.of(
            c("Employee Code", "كود الموظف", true, "String", "", "Unique across tenant"), c("Full Name", "الاسم الكامل", true, "String", "", "Min 3 characters"),
            c("National ID", "الرقم القومي", true, "String (14 Digits)", "", "Egyptian National ID"), c("Phone Number", "رقم الهاتف", false, "String", "", "International/local format"),
            c("Category Code", "كود الفئة", true, "String", "", "Must exist in Attendance Categories"), c("Department Code", "كود القسم", false, "String", "", "Must exist in Departments"),
            c("Branch Code", "كود الفرع", false, "String", "", "Must exist in Branches"), c("Base Salary", "الراتب الأساسي", false, "Decimal", "", ">= 0"),
            c("Hire Date", "تاريخ التعيين", true, "YYYY-MM-DD", "", "Past/current date"), c("Attendance Mode", "نمط الحضور", false, "Enum", "SCHEDULE_BASED, PUNCH_ONLY, HYBRID", "Default SCHEDULE_BASED"),
            c("Employment Type", "نوع التوظيف", false, "Enum", "FULL_TIME, PART_TIME, CONTRACTOR", "Default FULL_TIME"), c("Status", "الحالة", false, "Enum", "ACTIVE, INACTIVE", "Default ACTIVE")
        ), List.of(List.of("EMP-2001","Mahmoud Ibrahim Hassan","29401011234567","+201012345678","CAT_OFFICE","DEPT_ENG","BR_CAIRO",15000.00,"2025-03-01","SCHEDULE_BASED","FULL_TIME","ACTIVE")))),
        Map.entry("CHART_OF_ACCOUNTS", new Template("chart-of-accounts-sample.xlsx", List.of(
            c("Account Code","كود الحساب",true,"String","","Unique hierarchical code"), c("Account Name","اسم الحساب",true,"String","","Account title"), c("Account Type","نوع الحساب",true,"Enum","ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE","Exact enum"),
            c("Parent Account Code","كود الحساب الأب",false,"String","","Parent exists or above"), c("Currency","العملة",true,"ISO-4217","EGP, USD, EUR","3-letter currency"), c("Is Active","نشط",false,"Boolean","TRUE, FALSE","Default TRUE")
        ), List.of(List.of("101001","Main Cash Vault","ASSET","1010","EGP",true),List.of("201001","Trade Payables","LIABILITY","2010","EGP",true)))),
        Map.entry("BUSINESS_PARTIES", new Template("business-parties-sample.xlsx", List.of(
            c("Party Code","كود الشريك",true,"String","","Unique ID"), c("Party Name","اسم الشريك",true,"String","","Trading name"), c("Party Type","نوع الشريك",true,"Enum","SUPPLIER, CUSTOMER, BOTH","Exact enum"), c("Tax Number","الرقم الضريبي",false,"String","","Tax registration"),
            c("Commercial Reg","السجل التجاري",false,"String","","Commercial registration"), c("Phone Number","رقم الهاتف",false,"String","","Contact telephone"), c("Email","البريد الإلكتروني",false,"Email","","Valid email"), c("Address","العنوان",false,"String","","Physical address"),
            c("Currency","العملة",false,"ISO-4217","EGP, USD, EUR","Default currency"), c("Payment Terms Days","أيام السداد",false,"Integer","","Credit days")
        ), List.of(List.of("SUP-001","Al-Ahram Industrial Supplies","SUPPLIER","100-200-300","54321","+20223456789","finance@ahram-supplies.com","Smart Village, Giza","EGP",45)))),
        Map.entry("INVENTORY_ITEMS", new Template("inventory-items-sample.xlsx", List.of(
            c("Item Code","كود الصنف / SKU",true,"String","","Unique SKU"), c("Item Name","اسم الصنف",true,"String","","Descriptive name"), c("Category Code","كود الفئة",true,"String","","Master category"), c("Base UOM","وحدة القياس الأساسية",true,"String","PCS, KG, METER, BOX","Measurement unit"),
            c("Item Type","نوع الصنف",true,"Enum","RAW_MATERIAL, FINISHED_GOODS, SERVICE, CONSUMABLE","Exact enum"), c("Standard Cost","التكلفة المعيارية",false,"Decimal","",">= 0"), c("Default Sale Price","سعر البيع الافتراضي",false,"Decimal","",">= 0"), c("Reorder Point","حد إعادة الطلب",false,"Decimal","",">= 0"), c("Is Batch Tracked","تتبع برقم التشغيلة",false,"Boolean","TRUE, FALSE","")
        ), List.of(List.of("RM-STEEL-001","Stainless Steel Rod 10mm","RAW_METALS","KG","RAW_MATERIAL",120.50,175.00,50.0,true)))),
        Map.entry("BOM_MASTER", new Template("bom-master-sample.xlsx", List.of(
            c("Parent Item Code","كود الصنف النهائي",true,"String","","Must exist"), c("BOM Revision","إصدار شجرة المنتجات",true,"String","","Revision identifier"), c("Component Item Code","كود الصنف المكون",true,"String","","Must exist"), c("Quantity","الكمية",true,"Decimal","","> 0"), c("Component UOM","وحدة قياس المكون",true,"String","","Matches base UOM"), c("Scrap Percentage","نسبة الهالك %",false,"Decimal","","Wastage"), c("Work Center Code","كود مركز العمل",false,"String","","Work station")
        ), List.of(List.of("FG-WATER-PUMP","REV-A","RM-STEEL-001",3.25,"KG",2.5,"WC-ASSEMBLY")))),
        Map.entry("WORKERS", new Template("contractor-workers-sample.xlsx", List.of(
            c("Worker Code","كود العامل",true,"String","","Unique worker code"), c("Worker Name","اسم العامل",true,"String","","Full name"), c("National ID","الرقم القومي",true,"String (14 Digits)","","Egyptian ID"), c("Phone Number","رقم الهاتف",false,"String","","Contact phone"), c("Contractor Code","كود المقاول",true,"String","","BusinessParty code"), c("Category Code","كود فئة العمالة",true,"String","","Worker category"), c("Daily Rate","الأجر اليومي",true,"Decimal","",">= 0"), c("Status","الحالة",false,"Enum","ACTIVE, INACTIVE","Default ACTIVE")
        ), List.of(List.of("WRK-801","Sayed Ramadan Ali","28807051234567","+201200112233","CONTR-DELTA","MASONRY",400.00,"ACTIVE")))),
        Map.entry("WORKFORCE_ATTENDANCE", new Template("workforce-attendance-sample.xlsx", List.of(
            c("Work Date","تاريخ العمل",true,"YYYY-MM-DD","","Valid date"), c("Worker Code","كود العامل",true,"String","","Existing worker"), c("Contractor Code","كود المقاول",true,"String","","Assigned contractor"), c("Location Code","كود الموقع / المحطة",false,"String","","Site"), c("Hours Worked","ساعات العمل الفعلية",true,"Decimal","",">= 0"), c("Overtime Hours","ساعات إضافية",false,"Decimal","",">= 0"), c("Shift Type","نوع الوردية",false,"Enum","DAY, NIGHT","Default DAY"), c("Notes","ملاحظات",false,"String","","Optional")
        ), List.of(List.of("2026-08-15","WRK-801","CONTR-DELTA","SITE-NEW-CAPITAL",8.0,2.5,"DAY","Excavation task complete")))),
        Map.entry("BANK_STATEMENT", new Template("bank-statement-sample.xlsx", List.of(
            c("date","تاريخ المعاملة",true,"YYYY-MM-DD","","Current importer-compatible header"), c("valueDate","تاريخ القيمة",false,"YYYY-MM-DD","","Settlement date"), c("description","بيان الحركة / الوصف",true,"String","","Narrative"), c("reference","الرقم المرجعي / رقم الشيك",false,"String","","Bank reference"), c("amount","المبلغ",true,"Decimal","","Positive credit / negative debit; current importer compatible"), c("balance","الرصيد الدفتري",false,"Decimal","","Running balance")
        ), List.of(List.of("2026-08-10","2026-08-10","Wire Transfer In - Customer Inv 104","TRX-998821",85000.00,420150.00),List.of("2026-08-11","2026-08-11","Supplier Payment","TRX-998822",-12000.00,408150.00)))),
        Map.entry("TRANSLATIONS", new Template("translations-sample.xlsx", List.of(
            c("Translation Key","مفتاح الترجمة",true,"Dot notation","","Unique key"), c("Arabic Text","النص العربي",true,"UTF-8 String","","Arabic value"), c("English Text","النص الإنجليزي",true,"String","","English value"), c("Category","التصنيف",false,"String","","Functional module")
        ), List.of(List.of("finance.accounts.table.code","كود الحساب","Account Code","FINANCE"),List.of("common.save","حفظ","Save","COMMON")))),
        Map.entry("SUPPLIER_DOCUMENTS", new Template("supplier-document-requirements.xlsx", List.of(
            c("Document Type","نوع المستند",true,"Enum","COMMERCIAL_REGISTER, TAX_CARD, VAT_CERTIFICATE, BANK_LETTER","Reference catalog"), c("Arabic Label","الاسم العربي",true,"String","","Display label"), c("Required Metadata","البيانات المطلوبة",false,"String","","Metadata fields"), c("Allowed Extensions","الامتدادات المسموحة",true,"String",".pdf, .png, .jpg, .jpeg","Max 10 MB"), c("Max Size MB","الحد الأقصى بالميجابايت",true,"Integer","10","Per file")
        ), List.of(List.of("COMMERCIAL_REGISTER","السجل التجاري","Document Number; Expiry Date",".pdf, .png, .jpg, .jpeg",10),List.of("TAX_CARD","البطاقة الضريبية","Tax ID Number",".pdf, .png, .jpg, .jpeg",10),List.of("VAT_CERTIFICATE","شهادة التسجيل في ضريبة القيمة المضافة","",".pdf, .png, .jpg, .jpeg",10),List.of("BANK_LETTER","خطاب اعتماد الحساب البنكي الرسمي","",".pdf, .png, .jpg, .jpeg",10))))
    );

    public Template get(String key) {
        Template template = templates.get(key.toUpperCase(Locale.ROOT));
        if (template == null) throw new IllegalArgumentException("Unsupported sample template: " + key);
        return template;
    }
}
