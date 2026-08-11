package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.BomLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BomLineRepository extends JpaRepository<BomLine, String> {
    List<BomLine> findByBomHeaderIdOrderByLineNumberAsc(String bomHeaderId);
}
