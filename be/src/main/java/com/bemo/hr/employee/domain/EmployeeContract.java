package com.bemo.hr.employee.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee_contracts")
public class EmployeeContract {

    @Id
    private String id;

    @TenantId
    @Column(name = "app_id", nullable = false)
    private String appId;

    @Column(name = "contract_number", nullable = false, length = 50)
    private String contractNumber;

    @Column(name = "employee_id", nullable = false, length = 36)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 30)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContractStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "notice_period_days", nullable = false)
    private int noticePeriodDays;

    @Column(name = "basic_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "housing_allowance", nullable = false, precision = 15, scale = 2)
    private BigDecimal housingAllowance;

    @Column(name = "transportation_allowance", nullable = false, precision = 15, scale = 2)
    private BigDecimal transportationAllowance;

    @Column(name = "other_allowances", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherAllowances;

    @Column(name = "gross_salary", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "amendment_reason", length = 500)
    private String amendmentReason;

    @Column(name = "previous_contract_id", length = 36)
    private String previousContractId;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    protected EmployeeContract() {
    }

    public EmployeeContract(String contractNumber,
                            String employeeId,
                            ContractType contractType,
                            LocalDate startDate,
                            LocalDate endDate,
                            LocalDate probationEndDate,
                            int noticePeriodDays,
                            BigDecimal basicSalary,
                            BigDecimal housingAllowance,
                            BigDecimal transportationAllowance,
                            BigDecimal otherAllowances,
                            String jobTitle,
                            String departmentId,
                            String notes) {
        this.id = UUID.randomUUID().toString();
        this.contractNumber = contractNumber;
        this.employeeId = employeeId;
        this.contractType = contractType;
        this.status = ContractStatus.ACTIVE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.probationEndDate = probationEndDate;
        this.noticePeriodDays = noticePeriodDays > 0 ? noticePeriodDays : 30;
        this.basicSalary = basicSalary != null ? basicSalary : BigDecimal.ZERO;
        this.housingAllowance = housingAllowance != null ? housingAllowance : BigDecimal.ZERO;
        this.transportationAllowance = transportationAllowance != null ? transportationAllowance : BigDecimal.ZERO;
        this.otherAllowances = otherAllowances != null ? otherAllowances : BigDecimal.ZERO;
        this.grossSalary = this.basicSalary.add(this.housingAllowance)
                .add(this.transportationAllowance)
                .add(this.otherAllowances);
        this.jobTitle = jobTitle;
        this.departmentId = departmentId;
        this.notes = notes;
    }

    public void amend(String newContractNumber,
                      BigDecimal newBasicSalary,
                      BigDecimal newHousingAllowance,
                      BigDecimal newTransportationAllowance,
                      BigDecimal newOtherAllowances,
                      String newJobTitle,
                      LocalDate newEndDate,
                      String amendmentReason) {
        this.status = ContractStatus.AMENDED;
        this.amendmentReason = amendmentReason;
    }

    public void terminate(LocalDate terminationDate, String reason) {
        this.status = ContractStatus.TERMINATED;
        this.endDate = terminationDate;
        this.notes = (this.notes != null ? this.notes + " | " : "") + "Terminated: " + reason;
    }

    public void expire() {
        this.status = ContractStatus.EXPIRED;
    }

    @PrePersist
    void prePersist() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getProbationEndDate() {
        return probationEndDate;
    }

    public int getNoticePeriodDays() {
        return noticePeriodDays;
    }

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public BigDecimal getHousingAllowance() {
        return housingAllowance;
    }

    public BigDecimal getTransportationAllowance() {
        return transportationAllowance;
    }

    public BigDecimal getOtherAllowances() {
        return otherAllowances;
    }

    public BigDecimal getGrossSalary() {
        return grossSalary;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getNotes() {
        return notes;
    }

    public String getAmendmentReason() {
        return amendmentReason;
    }

    public String getPreviousContractId() {
        return previousContractId;
    }

    public void setPreviousContractId(String previousContractId) {
        this.previousContractId = previousContractId;
    }

    public long getVersion() {
        return version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
