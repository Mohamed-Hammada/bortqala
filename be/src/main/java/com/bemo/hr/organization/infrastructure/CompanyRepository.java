package com.bemo.hr.organization.infrastructure;

import com.bemo.hr.organization.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {
    List<Company> findAllByOrderByCodeAsc();
}
