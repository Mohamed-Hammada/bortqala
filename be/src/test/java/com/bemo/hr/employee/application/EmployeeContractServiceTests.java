package com.bemo.hr.employee.application;

import com.bemo.hr.employee.api.EmployeeContractApi;
import com.bemo.hr.employee.domain.ContractStatus;
import com.bemo.hr.employee.domain.ContractType;
import com.bemo.hr.employee.domain.Employee;
import com.bemo.hr.employee.domain.EmployeeContract;
import com.bemo.hr.employee.domain.EmploymentType;
import com.bemo.hr.employee.infrastructure.EmployeeContractRepository;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeContractServiceTests {

    private EmployeeContractRepository contractRepository;
    private EmployeeRepository employeeRepository;
    private EmployeeContractService contractService;

    @BeforeEach
    void setUp() {
        contractRepository = mock(EmployeeContractRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        contractService = new EmployeeContractService(contractRepository, employeeRepository);
    }

    @Test
    void createsContractSuccessfully() {
        Employee emp = new Employee("EMP-001", "Mohamed Ahmed", "DEV-01", "cat-1",
                EmploymentType.FIXED, new BigDecimal("10000"), LocalDate.of(2026, 1, 1), null, true);
        when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(emp));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeContractApi.CreateContractRequest request = new EmployeeContractApi.CreateContractRequest(
                "CNT-2026-001",
                ContractType.PERMANENT,
                LocalDate.of(2026, 1, 1),
                null,
                LocalDate.of(2026, 4, 1),
                30,
                new BigDecimal("8000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                "Senior Engineer",
                "dept-1",
                "Full time permanent contract"
        );

        EmployeeContractApi.ContractResponse resp = contractService.createContract("emp-1", request);
        assertThat(resp).isNotNull();
        assertThat(resp.contractNumber()).isEqualTo("CNT-2026-001");
        assertThat(resp.contractType()).isEqualTo(ContractType.PERMANENT);
        assertThat(resp.status()).isEqualTo(ContractStatus.ACTIVE);
        assertThat(resp.basicSalary()).isEqualByComparingTo(new BigDecimal("8000.00"));
        assertThat(resp.grossSalary()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    void amendsContractWithLineage() {
        EmployeeContract existing = new EmployeeContract(
                "CNT-2026-001", "emp-1", ContractType.PERMANENT,
                LocalDate.of(2026, 1, 1), null, null, 30,
                new BigDecimal("8000.00"), new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                "Engineer", "dept-1", "Original"
        );
        when(contractRepository.findById("cnt-1")).thenReturn(Optional.of(existing));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeContractApi.AmendContractRequest request = new EmployeeContractApi.AmendContractRequest(
                "CNT-2026-001-A1",
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                "Lead Engineer",
                null,
                "Annual Promotion"
        );

        EmployeeContractApi.ContractResponse resp = contractService.amendContract("cnt-1", request);
        assertThat(resp).isNotNull();
        assertThat(resp.contractNumber()).isEqualTo("CNT-2026-001-A1");
        assertThat(resp.grossSalary()).isEqualByComparingTo(new BigDecimal("13000.00"));
        assertThat(existing.getStatus()).isEqualTo(ContractStatus.AMENDED);
    }

    @Test
    void terminatesContract() {
        EmployeeContract contract = new EmployeeContract(
                "CNT-2026-001", "emp-1", ContractType.PERMANENT,
                LocalDate.of(2026, 1, 1), null, null, 30,
                new BigDecimal("8000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "Engineer", "dept-1", "Original"
        );
        when(contractRepository.findById("cnt-1")).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeContractApi.TerminateContractRequest request = new EmployeeContractApi.TerminateContractRequest(
                LocalDate.of(2026, 6, 30),
                "Resignation"
        );

        EmployeeContractApi.ContractResponse resp = contractService.terminateContract("cnt-1", request);
        assertThat(resp).isNotNull();
        assertThat(resp.status()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(resp.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }
}
