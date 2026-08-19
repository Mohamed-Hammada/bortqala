package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.ContractStatus;
import com.bemo.hr.employee.domain.EmployeeContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, String> {

    List<EmployeeContract> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    Optional<EmployeeContract> findFirstByEmployeeIdAndStatus(String employeeId, ContractStatus status);

    boolean existsByContractNumber(String contractNumber);

    List<EmployeeContract> findByStatusAndEndDateLessThanEqual(ContractStatus status, LocalDate date);
}
