package com.bemo.hr.organization.api;

import com.bemo.hr.organization.application.IntercompanyService;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.domain.Department;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.DepartmentRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final DepartmentRepository departmentRepository;
    private final IntercompanyService intercompanyService;

    public OrganizationController(
            CompanyRepository companyRepository,
            BranchRepository branchRepository,
            WarehouseRepository warehouseRepository,
            DepartmentRepository departmentRepository,
            IntercompanyService intercompanyService
    ) {
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.warehouseRepository = warehouseRepository;
        this.departmentRepository = departmentRepository;
        this.intercompanyService = intercompanyService;
    }

    @GetMapping
    public OrganizationApi.OrganizationHierarchyResponse getHierarchy() {
        var companies = companyRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
        var branches = branchRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
        var warehouses = warehouseRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
        var departments = departmentRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
        return new OrganizationApi.OrganizationHierarchyResponse(companies, branches, warehouses, departments);
    }

    // --- Companies ---
    @GetMapping("/companies")
    public List<OrganizationApi.CompanyResponse> listCompanies() {
        return companyRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/companies")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.CompanyResponse createCompany(@Valid @RequestBody OrganizationApi.CompanyPayload payload) {
        Company company = new Company(payload.code(), payload.name(), payload.taxNumber(), payload.commercialRegistry(), payload.active());
        return toResponse(companyRepository.save(company));
    }

    @PutMapping("/companies/{id}")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.CompanyResponse updateCompany(@PathVariable String id, @Valid @RequestBody OrganizationApi.CompanyPayload payload) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Company not found.", "ORG_COMPANY_NOT_FOUND", HttpStatus.CONFLICT));
        company.update(payload.code(), payload.name(), payload.taxNumber(), payload.commercialRegistry(), payload.active());
        return toResponse(companyRepository.save(company));
    }

    // --- Branches ---
    @GetMapping("/branches")
    public List<OrganizationApi.BranchResponse> listBranches() {
        return branchRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/branches")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.BranchResponse createBranch(@Valid @RequestBody OrganizationApi.BranchPayload payload) {
        Branch branch = new Branch(payload.companyId(), payload.code(), payload.name(), payload.location(), payload.active());
        return toResponse(branchRepository.save(branch));
    }

    @PutMapping("/branches/{id}")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.BranchResponse updateBranch(@PathVariable String id, @Valid @RequestBody OrganizationApi.BranchPayload payload) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Branch not found.", "ORG_BRANCH_NOT_FOUND", HttpStatus.CONFLICT));
        branch.update(payload.companyId(), payload.code(), payload.name(), payload.location(), payload.active());
        return toResponse(branchRepository.save(branch));
    }

    // --- Warehouses ---
    @GetMapping("/warehouses")
    public List<OrganizationApi.WarehouseResponse> listWarehouses() {
        return warehouseRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/warehouses")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.WarehouseResponse createWarehouse(@Valid @RequestBody OrganizationApi.WarehousePayload payload) {
        Warehouse warehouse = new Warehouse(payload.branchId(), payload.code(), payload.name(), payload.location(), payload.active());
        return toResponse(warehouseRepository.save(warehouse));
    }

    @PutMapping("/warehouses/{id}")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.WarehouseResponse updateWarehouse(@PathVariable String id, @Valid @RequestBody OrganizationApi.WarehousePayload payload) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Warehouse not found.", "ORG_WAREHOUSE_NOT_FOUND", HttpStatus.CONFLICT));
        warehouse.update(payload.branchId(), payload.code(), payload.name(), payload.location(), payload.active());
        return toResponse(warehouseRepository.save(warehouse));
    }

    // --- Departments ---
    @GetMapping("/departments")
    public List<OrganizationApi.DepartmentResponse> listDepartments() {
        return departmentRepository.findAllByOrderByCodeAsc().stream().map(this::toResponse).toList();
    }

    @PostMapping("/departments")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.DepartmentResponse createDepartment(@Valid @RequestBody OrganizationApi.DepartmentPayload payload) {
        Department department = new Department(payload.companyId(), payload.code(), payload.name(), payload.managerId(), payload.active());
        return toResponse(departmentRepository.save(department));
    }

    @PutMapping("/departments/{id}")
    @Transactional
    @PreAuthorize("@auth.hasPermission('organization.manage')")
    public OrganizationApi.DepartmentResponse updateDepartment(@PathVariable String id, @Valid @RequestBody OrganizationApi.DepartmentPayload payload) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Department not found.", "ORG_DEPARTMENT_NOT_FOUND", HttpStatus.CONFLICT));
        department.update(payload.companyId(), payload.code(), payload.name(), payload.managerId(), payload.active());
        return toResponse(departmentRepository.save(department));
    }

    // --- Multi-Branch Financial Consolidation & Intercompany ---

    @GetMapping("/consolidation/summary")
    public OrganizationApi.ConsolidatedOrganizationSummary getConsolidatedSummary() {
        return intercompanyService.getConsolidatedSummary();
    }

    @GetMapping("/intercompany")
    public List<OrganizationApi.IntercompanyTransactionResponse> listIntercompanyTransactions() {
        return intercompanyService.listTransactions();
    }

    @PostMapping("/intercompany")
    @PreAuthorize("@auth.hasAnyPermission('organization.manage', 'finance.manage')")
    public OrganizationApi.IntercompanyTransactionResponse createIntercompanyTransaction(
            @Valid @RequestBody OrganizationApi.CreateIntercompanyPayload payload
    ) {
        return intercompanyService.createTransaction(payload);
    }

    @PostMapping("/intercompany/{id}/approve")
    @PreAuthorize("@auth.hasAnyPermission('organization.manage', 'finance.manage')")
    public OrganizationApi.IntercompanyTransactionResponse approveIntercompanyTransaction(@PathVariable String id) {
        return intercompanyService.approveTransaction(id);
    }

    @PostMapping("/intercompany/{id}/settle")
    @PreAuthorize("@auth.hasAnyPermission('organization.manage', 'finance.manage')")
    public OrganizationApi.IntercompanyTransactionResponse settleIntercompanyTransaction(@PathVariable String id) {
        return intercompanyService.settleTransaction(id);
    }

    @PostMapping("/intercompany/eliminate")
    @PreAuthorize("@auth.hasAnyPermission('organization.manage', 'finance.manage')")
    public OrganizationApi.EliminationResultResponse runPeriodElimination(
            @Valid @RequestBody OrganizationApi.RunEliminationPayload payload
    ) {
        return intercompanyService.runPeriodElimination(payload.period());
    }

    private OrganizationApi.CompanyResponse toResponse(Company c) {
        return new OrganizationApi.CompanyResponse(c.getId(), c.getCode(), c.getName(), c.getTaxNumber(), c.getCommercialRegistry(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private OrganizationApi.BranchResponse toResponse(Branch b) {
        return new OrganizationApi.BranchResponse(b.getId(), b.getCompanyId(), b.getCode(), b.getName(), b.getLocation(), b.isActive(), b.getCreatedAt(), b.getUpdatedAt());
    }

    private OrganizationApi.WarehouseResponse toResponse(Warehouse w) {
        return new OrganizationApi.WarehouseResponse(w.getId(), w.getBranchId(), w.getCode(), w.getName(), w.getLocation(), w.isActive(), w.getCreatedAt(), w.getUpdatedAt());
    }

    private OrganizationApi.DepartmentResponse toResponse(Department d) {
        return new OrganizationApi.DepartmentResponse(d.getId(), d.getCompanyId(), d.getCode(), d.getName(), d.getManagerId(), d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
