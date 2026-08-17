package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.BomHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomHeaderRepository extends JpaRepository<BomHeader, String> {
    List<BomHeader> findAllByOrderByBomCodeAsc();

    boolean existsByBomCodeIgnoreCase(String bomCode);
}
