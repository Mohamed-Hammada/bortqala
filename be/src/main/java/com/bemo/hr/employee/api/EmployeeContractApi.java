package com.bemo.hr.employee.api;

import com.bemo.hr.employee.domain.ContractStatus;
import com.bemo.hr.employee.domain.ContractType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmployeeContractApi {

    private EmployeeContractApi() {
    }

    public record CreateContractRequest(
            @Size(max = 50) String contractNumber,
            @NotNull ContractType contractType,
            @NotNull LocalDate startDate,
            LocalDate endDate,
            LocalDate probationEndDate,
            int noticePeriodDays,
            @NotNull BigDecimal basicSalary,
            BigDecimal housingAllowance,
            BigDecimal transportationAllowance,
            BigDecimal otherAllowances,
            String jobTitle,
            String departmentId,
            String notes
    ) {
    }

    public record AmendContractRequest(
            @Size(max = 50) String newContractNumber,
            @NotNull BigDecimal basicSalary,
            BigDecimal housingAllowance,
            BigDecimal transportationAllowance,
            BigDecimal otherAllowances,
            String jobTitle,
            LocalDate endDate,
            @NotBlank String amendmentReason
    ) {
    }

    public record TerminateContractRequest(
            @NotNull LocalDate terminationDate,
            @NotBlank String reason
    ) {
    }

    public record ContractResponse(
            String id,
            String contractNumber,
            String employeeId,
            ContractType contractType,
            ContractStatus status,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate probationEndDate,
            int noticePeriodDays,
            BigDecimal basicSalary,
            BigDecimal housingAllowance,
            BigDecimal transportationAllowance,
            BigDecimal otherAllowances,
            BigDecimal grossSalary,
            String jobTitle,
            String departmentId,
            String notes,
            String amendmentReason,
            String previousContractId,
            long createdAt,
            long updatedAt,
            long version
    ) {
    }
}
