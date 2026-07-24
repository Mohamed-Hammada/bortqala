package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.AttendanceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceCategoryRepository extends JpaRepository<AttendanceCategory, String> {
    List<AttendanceCategory> findAllByOrderByNameAsc();
    Optional<AttendanceCategory> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
    boolean existsByCodeIgnoreCase(String code);
}
