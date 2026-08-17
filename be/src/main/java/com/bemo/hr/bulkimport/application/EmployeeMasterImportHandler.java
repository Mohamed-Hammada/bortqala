package com.bemo.hr.bulkimport.application;

import com.bemo.hr.bulkimport.domain.SmartImportModels.CellError;
import com.bemo.hr.bulkimport.domain.SmartImportModels.HandlerOutcome;
import com.bemo.hr.bulkimport.domain.SmartImportModels.PreviewRow;
import com.bemo.hr.bulkimport.domain.SmartImportModels.Workflow;
import com.bemo.hr.employee.api.EmployeeApi;
import com.bemo.hr.employee.application.HrConfigurationService;
import com.bemo.hr.employee.domain.EmploymentType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class EmployeeMasterImportHandler implements SmartImportHandler {
    private final HrConfigurationService hr;
    private final TransactionTemplate strictTransaction;
    private final TransactionTemplate rowTransaction;

    public EmployeeMasterImportHandler(HrConfigurationService hr, PlatformTransactionManager transactionManager) {
        this.hr = hr;
        this.strictTransaction = new TransactionTemplate(transactionManager);
        this.rowTransaction = new TransactionTemplate(transactionManager);
        this.rowTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean supports(String workflowKey) {
        return "employees".equals(workflowKey);
    }

    @Override
    public HandlerOutcome commit(Workflow workflow, List<PreviewRow> rows, boolean skipInvalid) {
        var personal = byEmployeeCode(rows, "personal");
        var employment = byEmployeeCode(rows, "employment");
        var salary = byEmployeeCode(rows, "salary");
        var categories = hr.listCategories();
        var existingCodes = new HashSet<String>();
        hr.listEmployees().forEach(employee -> {
            if (employee.employeeCode() != null)
                existingCodes.add(employee.employeeCode().strip().toLowerCase(Locale.ROOT));
        });

        var errors = new ArrayList<CellError>();
        var candidates = new ArrayList<PreparedEmployee>();
        for (var entry : personal.entrySet()) {
            String employeeCode = entry.getKey();
            var p = entry.getValue();
            var e = employment.get(employeeCode);
            var s = salary.get(employeeCode);
            if (e == null) {
                errors.add(error(p, "EmployeeCode", "Employment sheet row is required for this employee.", "يجب وجود سطر للموظف في صفحة التوظيف."));
                continue;
            }
            if (existingCodes.contains(employeeCode.toLowerCase(Locale.ROOT))) {
                errors.add(error(p, "EmployeeCode", "Employee code already exists in Bemo ERP.", "كود الموظف موجود بالفعل في النظام."));
                continue;
            }
            String categoryInput = clean(e.values().get("AssignedAttendanceCategory"));
            var category = categories.stream()
                    .filter(c -> c.code().equalsIgnoreCase(categoryInput) || c.name().equalsIgnoreCase(categoryInput))
                    .findFirst().orElse(null);
            if (category == null) {
                errors.add(error(e, "AssignedAttendanceCategory", "Attendance category was not found by code or name.", "لم يتم العثور على فئة الحضور بالكود أو الاسم."));
                continue;
            }
            if (category.attendanceMode().name().equals("BIOMETRIC") && clean(p.values().get("DeviceUserId")).isBlank()) {
                errors.add(error(p, "DeviceUserId", "Active employees in biometric categories require a biometric DeviceUserId in the current branch schema.",
                        "الموظف النشط في فئة بصمة يحتاج DeviceUserId وفق نموذج الفرع الحالي."));
                continue;
            }
            var request = new EmployeeApi.UpsertRequest(
                    employeeCode,
                    fullName(p),
                    blankToNull(p.values().get("DeviceUserId")),
                    category.id(),
                    employmentType(e.values().get("ContractType")),
                    s == null ? null : decimalOrNull(s.values().get("BaseSalary")),
                    LocalDate.parse(clean(e.values().get("HireDate"))),
                    null,
                    true,
                    null);
            candidates.add(new PreparedEmployee(p, request));
        }

        if (!skipInvalid && !errors.isEmpty()) {
            throw new IllegalArgumentException("Strict employee import blocked by current-schema/domain validation: " + errors.get(0).messageEn());
        }

        int committed;
        if (skipInvalid) {
            committed = 0;
            for (var candidate : candidates) {
                try {
                    rowTransaction.executeWithoutResult(status -> hr.createEmployee(candidate.request()));
                    committed++;
                } catch (RuntimeException ex) {
                    errors.add(error(candidate.source(), "EmployeeCode", "Domain validation rejected employee: " + safeMessage(ex),
                            "رفضت قواعد النظام الموظف: " + safeMessage(ex)));
                }
            }
        } else {
            strictTransaction.executeWithoutResult(status -> candidates.forEach(candidate -> hr.createEmployee(candidate.request())));
            committed = candidates.size();
        }

        return new HandlerOutcome(true, committed, errors.size(),
                "Employees were committed using the current branch employee schema. National ID, banking, department/branch, job title and allowance fields remain validation/staging metadata because those fields are not present in the current Employee write model.",
                "تم حفظ الموظفين باستخدام نموذج الموظف الحالي في الفرع. تبقى بيانات الرقم القومي والبنك والإدارة/الفرع والمسمى الوظيفي والبدلات كبيانات تحقق/مرحلية لأن نموذج Employee الحالي لا يحتوي هذه الحقول.",
                errors);
    }

    private Map<String, PreviewRow> byEmployeeCode(List<PreviewRow> rows, String sheet) {
        var result = new HashMap<String, PreviewRow>();
        for (var row : rows)
            if (sheet.equals(row.sheet())) {
                var code = clean(row.values().get("EmployeeCode"));
                if (!code.isBlank()) result.put(code, row);
            }
        return result;
    }

    private String fullName(PreviewRow row) {
        String ar = (clean(row.values().get("FirstNameAr")) + " " + clean(row.values().get("LastNameAr"))).strip();
        String en = (clean(row.values().get("FirstNameEn")) + " " + clean(row.values().get("LastNameEn"))).strip();
        return !ar.isBlank() ? ar : en;
    }

    private EmploymentType employmentType(String value) {
        return "DAILY".equalsIgnoreCase(clean(value)) ? EmploymentType.DAILY : EmploymentType.FIXED;
    }

    private BigDecimal decimalOrNull(String value) {
        return clean(value).isBlank() ? null : new BigDecimal(clean(value).replace(",", ""));
    }

    private String blankToNull(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? null : cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private CellError error(PreviewRow row, String column, String en, String ar) {
        return new CellError(row.rowNumber(), row.sheet(), column, en, ar);
    }

    private record PreparedEmployee(PreviewRow source, EmployeeApi.UpsertRequest request) {
    }
}
