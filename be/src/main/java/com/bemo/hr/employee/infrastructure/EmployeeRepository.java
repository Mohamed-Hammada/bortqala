package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findAllByOrderByFullNameAsc();

    Optional<Employee> findByDeviceUserId(String deviceUserId);

    List<Employee> findByDeviceUserIdIn(Collection<String> deviceUserIds);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);

    boolean existsByEmployeeCodeIgnoreCase(String code);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String code, String id);

    boolean existsByDeviceUserId(String deviceUserId);

    boolean existsByDeviceUserIdAndIdNot(String deviceUserId, String id);

    boolean existsByCategoryIdAndActiveTrue(String categoryId);

    @Modifying
    @Query(value = """
            INSERT INTO employees (
                id, app_id, employee_code, full_name, device_user_id, category_id,
                employment_type, base_salary, active_from, active_to, active,
                version, created_at, updated_at
            ) VALUES (
                :id, :appId, :employeeCode, :fullName, :deviceUserId, :categoryId,
                :employmentType, 0, :activeFrom, NULL, :active,
                0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertAutoProvisioned(@Param("id") String id,
                              @Param("appId") String appId,
                              @Param("employeeCode") String employeeCode,
                              @Param("fullName") String fullName,
                              @Param("deviceUserId") String deviceUserId,
                              @Param("categoryId") String categoryId,
                              @Param("employmentType") String employmentType,
                              @Param("activeFrom") LocalDate activeFrom,
                              @Param("active") boolean active);

    List<Employee> findByActiveTrueAndActiveFromLessThanEqualAndActiveToIsNullOrActiveTrueAndActiveFromLessThanEqualAndActiveToGreaterThanEqual(
            LocalDate from1, LocalDate from2, LocalDate to);
}
