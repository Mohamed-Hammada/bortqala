package com.bemo.hr.payroll.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_run_lines")
public class PayrollRunLine {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Column(name = "snapshot_id", length = 36)
    private String snapshotId;

    @Column(name = "basic_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal allowances;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal deductions;

    @Column(name = "net_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal netSalary;

    protected PayrollRunLine() {}

    public PayrollRunLine(String runId, String employeeId, BigDecimal basicSalary, BigDecimal allowances, BigDecimal deductions) {
        this(runId, employeeId, null, basicSalary, allowances, deductions);
    }

    public PayrollRunLine(String runId, String employeeId, String snapshotId, BigDecimal basicSalary, BigDecimal allowances, BigDecimal deductions) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.employeeId = employeeId;
        this.snapshotId = snapshotId;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;
        this.netSalary = basicSalary.add(allowances).subtract(deductions);
    }

    public String getId() { return id; }
    public String getAppId() { return appId; }
    public String getRunId() { return runId; }
    public String getEmployeeId() { return employeeId; }
    public String getSnapshotId() { return snapshotId; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getAllowances() { return allowances; }
    public BigDecimal getDeductions() { return deductions; }
    public BigDecimal getNetSalary() { return netSalary; }
}
