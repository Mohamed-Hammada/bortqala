package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findAllByOrderByFullNameAsc();
    Optional<Employee> findByDeviceUserId(String deviceUserId);
    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);
    boolean existsByEmployeeCodeIgnoreCase(String code);
    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String code, String id);
    boolean existsByDeviceUserId(String deviceUserId);
    boolean existsByDeviceUserIdAndIdNot(String deviceUserId, String id);
    boolean existsByCategoryIdAndActiveTrue(String categoryId);
    List<Employee> findByActiveTrueAndActiveFromLessThanEqualAndActiveToIsNullOrActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqual(
            LocalDate from1, LocalDate from2, LocalDate to);
}
