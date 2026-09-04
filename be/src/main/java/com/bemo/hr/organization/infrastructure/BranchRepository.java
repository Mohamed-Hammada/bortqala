package com.bemo.hr.organization.infrastructure;

import com.bemo.hr.organization.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findAllByOrderByCodeAsc();

    List<Branch> findByCompanyIdOrderByCodeAsc(String companyId);

    List<Branch> findByActiveTrueOrderByCodeAsc();

    Optional<Branch> findByCodeIgnoreCase(String code);

    Optional<Branch> findByIsMainBranchTrue();
}
