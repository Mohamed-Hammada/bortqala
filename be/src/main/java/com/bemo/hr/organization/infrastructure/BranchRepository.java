package com.bemo.hr.organization.infrastructure;

import com.bemo.hr.organization.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findAllByOrderByCodeAsc();

    List<Branch> findByCompanyIdOrderByCodeAsc(String companyId);
}
