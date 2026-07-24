package com.bemo.hr.employee.infrastructure;

import com.bemo.hr.employee.domain.AttendanceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceCategoryRepository extends JpaRepository<AttendanceCategory, String> {
    List<AttendanceCategory> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
    boolean existsByCodeIgnoreCase(String code);
}
