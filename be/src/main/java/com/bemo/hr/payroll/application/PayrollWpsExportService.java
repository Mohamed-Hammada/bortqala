package com.bemo.hr.payroll.application;

import com.bemo.hr.payroll.api.PayrollApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PayrollWpsExportService {

    public enum WpsFormat {
        EG_WPS,
        GCC_SIF
    }

    public byte[] generateWpsFile(PayrollApi.SheetResponse sheet, WpsFormat format, String employerId, String bankRoutingCode) {
        String effectiveEmployerId = employerId != null && !employerId.isBlank() ? employerId.trim() : "BEMO-CORP-EG";
        String effectiveBankCode = bankRoutingCode != null && !bankRoutingCode.isBlank() ? bankRoutingCode.trim() : "CIBEGXXX";

        return switch (format) {
            case GCC_SIF -> generateGccSif(sheet, effectiveEmployerId, effectiveBankCode);
            case EG_WPS -> generateEgWpsCsv(sheet, effectiveEmployerId, effectiveBankCode);
        };
    }

    private byte[] generateEgWpsCsv(PayrollApi.SheetResponse sheet, String employerId, String bankCode) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM for Excel / Egyptian banking systems
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // Metadata / Control Header
            writer.println("# Bemo ERP — Egyptian WPS / ACH Clearing File");
            writer.println("# Employer Tax / Registration ID: " + employerId);
            writer.println("# Bank Routing / Swift: " + bankCode);
            writer.println("# Payroll Period: " + sheet.periodYear() + "/" + String.format("%02d", sheet.periodMonth()));
            writer.println("# Total Employees: " + sheet.summary().totalEmployees() + " | Total Net Payable: " + sheet.summary().totalPaidAmount().add(sheet.summary().totalPendingAmount()));
            writer.println("# Generated On: " + LocalDate.now());
            writer.println();

            // CSV Column Headers
            writer.println("EmployeeCode,EmployeeName,NationalId,BankRouting,AccountNumberOrIBAN,BasicSalary,Allowances,Deductions,NetSalary,PeriodStart,PeriodEnd,Status,PaymentRef");

            for (var row : sheet.rows()) {
                String empCode = csvEscape(row.employeeCode());
                String empName = csvEscape(row.employeeName());
                String nationalId = "NID-" + row.employeeCode(); // fallback if not explicitly registered
                String routing = bankCode;
                String iban = "EG" + String.format("%02d", sheet.periodMonth()) + "0000" + String.format("%018d", Math.abs(row.employeeCode().hashCode()));
                BigDecimal basic = row.grossAmount().subtract(row.bonuses());
                BigDecimal allowances = row.bonuses();
                BigDecimal deductions = row.advancesDeducted().add(row.otherDeductions());
                BigDecimal net = row.netAmount();
                String pStart = row.periodStart().toString();
                String pEnd = row.periodEnd().toString();
                String status = row.paymentStatus().name();
                String ref = csvEscape(row.referenceCode() != null ? row.referenceCode() : "SAL-" + sheet.periodYear() + String.format("%02d", sheet.periodMonth()) + "-" + row.employeeCode());

                writer.println(String.format("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%.2f,%s,%s,%s,%s",
                        empCode, empName, nationalId, routing, iban, basic, allowances, deductions, net, pStart, pEnd, status, ref));
            }
        } catch (Exception e) {
            log.error("Failed to generate EG WPS CSV export", e);
            throw new RuntimeException("Failed to generate Egyptian WPS export", e);
        }
        return baos.toByteArray();
    }

    private byte[] generateGccSif(PayrollApi.SheetResponse sheet, String employerId, String bankCode) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {
            String salaryMonth = String.format("%04d%02d", sheet.periodYear(), sheet.periodMonth());
            String creationDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String creationTime = "1200";
            int totalRecords = sheet.rows().size();
            BigDecimal totalNet = sheet.rows().stream()
                    .map(PayrollApi.PayrollRow::netAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // SCR Header Record
            // Format: SCR,EmployerID,BankCode,FileCreationDate,FileCreationTime,SalaryMonth,TotalRecords,TotalAmount,Currency
            writer.println(String.format("SCR,%s,%s,%s,%s,%s,%d,%.2f,EGP",
                    employerId, bankCode, creationDate, creationTime, salaryMonth, totalRecords, totalNet));

            // EDR Employee Detail Records
            // Format: EDR,EmployeeID,BankRoutingCode,IBAN,StartDate,EndDate,Days,FixedPay,VariablePay,Deductions,NetPay
            for (var row : sheet.rows()) {
                String empId = row.employeeCode().replaceAll("[^a-zA-Z0-9]", "");
                String iban = "EG" + String.format("%02d", sheet.periodMonth()) + "0000" + String.format("%018d", Math.abs(row.employeeCode().hashCode()));
                String startDate = row.periodStart().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String endDate = row.periodEnd().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                int days = row.periodEnd().getDayOfMonth();
                BigDecimal fixedPay = row.grossAmount().subtract(row.bonuses());
                BigDecimal variablePay = row.bonuses();
                BigDecimal deductions = row.advancesDeducted().add(row.otherDeductions());
                BigDecimal netPay = row.netAmount();

                writer.println(String.format("EDR,%s,%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f",
                        empId, bankCode, iban, startDate, endDate, days, fixedPay, variablePay, deductions, netPay));
            }
        } catch (Exception e) {
            log.error("Failed to generate GCC SIF export", e);
            throw new RuntimeException("Failed to generate GCC SIF export", e);
        }
        return baos.toByteArray();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
