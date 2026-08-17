package com.bemo.hr.organization.infrastructure;

import com.bemo.hr.organization.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    List<Department> findAllByOrderByCodeAsc();

    List<Department> findByCompanyIdOrderByCodeAsc(String companyId);
}
