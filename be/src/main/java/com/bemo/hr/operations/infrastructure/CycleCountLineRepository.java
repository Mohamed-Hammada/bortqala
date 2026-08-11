package com.bemo.hr.operations.infrastructure;

import com.bemo.hr.operations.domain.CycleCountLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CycleCountLineRepository extends JpaRepository<CycleCountLine, String> {
    List<CycleCountLine> findByCountId(String countId);
}
