package com.bemo.hr.organization.infrastructure;

import com.bemo.hr.organization.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findAllByOrderByCodeAsc();

    List<Warehouse> findByBranchIdOrderByCodeAsc(String branchId);
}
