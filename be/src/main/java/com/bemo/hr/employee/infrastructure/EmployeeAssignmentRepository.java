package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.EmployeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeAssignmentRepository extends JpaRepository<EmployeeAssignment, String> {
    List<EmployeeAssignment> findByEmployeeIdOrderByEffectiveFromDesc(String employeeId);

    EmployeeAssignment findFirstByEmployeeIdAndEffectiveToIsNullOrderByEffectiveFromDesc(String employeeId);

    long deleteByEmployeeIdIn(java.util.Collection<String> employeeIds);
}
