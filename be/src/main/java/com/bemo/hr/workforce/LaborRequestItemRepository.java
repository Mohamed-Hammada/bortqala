package com.bemo.hr.workforce;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LaborRequestItemRepository extends JpaRepository<LaborRequestItem, String> {
    List<LaborRequestItem> findByRequestId(String requestId);
}
