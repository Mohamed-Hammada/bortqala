package com.bemo.hr.employee.api;

import com.bemo.hr.employee.domain.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class EmployeeApi {
    private EmployeeApi() {
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 50) String employeeCode,
            @NotBlank @Size(max = 200) String fullName,
            @Size(max = 100) String deviceUserId,
            @NotBlank String categoryId,
            @NotNull EmploymentType employmentType,
            @NotNull LocalDate activeFrom,
            LocalDate activeTo,
            boolean active,
            Long version) {
    }

    public record Response(
            String id,
            String employeeCode,
            String fullName,
            String deviceUserId,
            String categoryId,
            String categoryName,
            EmploymentType employmentType,
            LocalDate activeFrom,
            LocalDate activeTo,
            boolean active,
            long version) {
    }
}
