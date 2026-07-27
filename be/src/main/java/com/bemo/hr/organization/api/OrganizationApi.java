package com.bemo.hr.organization.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class OrganizationApi {

    public record CompanyResponse(
            String id,
            String code,
            String name,
            String taxNumber,
            String commercialRegistry,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record CompanyPayload(
            @NotBlank String code,
            @NotBlank String name,
            String taxNumber,
            String commercialRegistry,
            boolean active
    ) {}

    public record BranchResponse(
            String id,
            String companyId,
            String code,
            String name,
            String location,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record BranchPayload(
            @NotBlank String companyId,
            @NotBlank String code,
            @NotBlank String name,
            String location,
            boolean active
    ) {}

    public record WarehouseResponse(
            String id,
            String branchId,
            String code,
            String name,
            String location,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record WarehousePayload(
            @NotBlank String branchId,
            @NotBlank String code,
            @NotBlank String name,
            String location,
            boolean active
    ) {}

    public record DepartmentResponse(
            String id,
            String companyId,
            String code,
            String name,
            String managerId,
            boolean active,
            long createdAt,
            long updatedAt
    ) {}

    public record DepartmentPayload(
            @NotBlank String companyId,
            @NotBlank String code,
            @NotBlank String name,
            String managerId,
            boolean active
    ) {}

    public record OrganizationHierarchyResponse(
            List<CompanyResponse> companies,
            List<BranchResponse> branches,
            List<WarehouseResponse> warehouses,
            List<DepartmentResponse> departments
    ) {}
}
