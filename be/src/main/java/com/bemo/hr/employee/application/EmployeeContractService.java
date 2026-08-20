package com.bemo.hr.employee.application;

import com.bemo.hr.employee.api.EmployeeContractApi;
import com.bemo.hr.employee.domain.ContractStatus;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmployeeContract;
import com.bemo.hr.employee.infrastructure.EmployeeContractRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class EmployeeContractService {

    private final EmployeeContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;

    public EmployeeContractService(EmployeeContractRepository contractRepository,
                                   EmployeeRepository employeeRepository) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeContractApi.ContractResponse> listContractsForEmployee(String employeeId) {
        log.debug("listContractsForEmployee called with employeeId={}", employeeId);
        return contractRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeContractApi.ContractResponse getContract(String id) {
        log.debug("getContract called with id={}", id);
        EmployeeContract contract = contractRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Employment contract not found", "CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toResponse(contract);
    }

    @Transactional
    public EmployeeContractApi.ContractResponse createContract(String employeeId, EmployeeContractApi.CreateContractRequest request) {
        log.debug("createContract called for employeeId={}, contractNumber={}", employeeId, request.contractNumber());
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessRuleException("Employee not found", "EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND));

        String contractNumber = request.contractNumber();
        if (contractNumber == null || contractNumber.isBlank()) {
            contractNumber = generateContractNumber();
        } else if (contractRepository.existsByContractNumber(contractNumber)) {
            throw new BusinessRuleException("Contract number already exists", "CONTRACT_NUMBER_EXISTS", HttpStatus.BAD_REQUEST);
        }

        // Deactivate existing active contracts for the employee
        contractRepository.findFirstByEmployeeIdAndStatus(employeeId, ContractStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.expire();
                    contractRepository.save(existing);
                });

        EmployeeContract contract = new EmployeeContract(
                contractNumber,
                employeeId,
                request.contractType(),
                request.startDate(),
                request.endDate(),
                request.probationEndDate(),
                request.noticePeriodDays(),
                request.basicSalary(),
                request.housingAllowance(),
                request.transportationAllowance(),
                request.otherAllowances(),
                request.jobTitle(),
                request.departmentId(),
                request.notes()
        );

        EmployeeContract saved = contractRepository.save(contract);
        log.info("Contract {} created for employee {}", saved.getContractNumber(), employeeId);
        return toResponse(saved);
    }

    @Transactional
    public EmployeeContractApi.ContractResponse amendContract(String contractId, EmployeeContractApi.AmendContractRequest request) {
        log.debug("amendContract called for contractId={}", contractId);
        EmployeeContract existing = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessRuleException("Employment contract not found", "CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (existing.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessRuleException("Only active contracts can be modified", "CONTRACT_NOT_ACTIVE", HttpStatus.BAD_REQUEST);
        }

        String newContractNumber = request.newContractNumber();
        if (newContractNumber == null || newContractNumber.isBlank()) {
            newContractNumber = generateContractNumber();
        }

        existing.amend(
                newContractNumber,
                request.basicSalary(),
                request.housingAllowance(),
                request.transportationAllowance(),
                request.otherAllowances(),
                request.jobTitle(),
                request.endDate(),
                request.amendmentReason()
        );
        contractRepository.save(existing);

        // Create new active amended contract record referencing the previous contract
        EmployeeContract amendedContract = new EmployeeContract(
                newContractNumber,
                existing.getEmployeeId(),
                existing.getContractType(),
                LocalDate.now(),
                request.endDate() != null ? request.endDate() : existing.getEndDate(),
                existing.getProbationEndDate(),
                existing.getNoticePeriodDays(),
                request.basicSalary(),
                request.housingAllowance(),
                request.transportationAllowance(),
                request.otherAllowances(),
                request.jobTitle() != null ? request.jobTitle() : existing.getJobTitle(),
                existing.getDepartmentId(),
                "Amended from " + existing.getContractNumber() + ": " + request.amendmentReason()
        );
        amendedContract.setPreviousContractId(existing.getId());

        EmployeeContract saved = contractRepository.save(amendedContract);
        log.info("Contract {} amended to new contract {}", existing.getContractNumber(), saved.getContractNumber());
        return toResponse(saved);
    }

    @Transactional
    public EmployeeContractApi.ContractResponse terminateContract(String contractId, EmployeeContractApi.TerminateContractRequest request) {
        log.debug("terminateContract called for contractId={}", contractId);
        EmployeeContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BusinessRuleException("Employment contract not found", "CONTRACT_NOT_FOUND", HttpStatus.NOT_FOUND));

        contract.terminate(request.terminationDate(), request.reason());
        EmployeeContract saved = contractRepository.save(contract);
        log.info("Contract {} terminated as of {}", contract.getContractNumber(), request.terminationDate());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeContractApi.ContractResponse> listExpiringContracts(int withinDays) {
        LocalDate threshold = LocalDate.now().plusDays(withinDays > 0 ? withinDays : 30);
        return contractRepository.findByStatusAndEndDateLessThanEqual(ContractStatus.ACTIVE, threshold)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateContractNumber() {
        int year = LocalDate.now().getYear();
        int seq = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("CNT-%d-%04d", year, seq);
    }

    private EmployeeContractApi.ContractResponse toResponse(EmployeeContract c) {
        return new EmployeeContractApi.ContractResponse(
                c.getId(),
                c.getContractNumber(),
                c.getEmployeeId(),
                c.getContractType(),
                c.getStatus(),
                c.getStartDate(),
                c.getEndDate(),
                c.getProbationEndDate(),
                c.getNoticePeriodDays(),
                c.getBasicSalary(),
                c.getHousingAllowance(),
                c.getTransportationAllowance(),
                c.getOtherAllowances(),
                c.getGrossSalary(),
                c.getJobTitle(),
                c.getDepartmentId(),
                c.getNotes(),
                c.getAmendmentReason(),
                c.getPreviousContractId(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getVersion()
        );
    }
}
