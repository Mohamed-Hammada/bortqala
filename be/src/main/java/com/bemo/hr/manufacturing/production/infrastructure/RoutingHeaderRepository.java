package com.bemo.hr.manufacturing.production.infrastructure;

import com.bemo.hr.manufacturing.production.domain.RoutingHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingHeaderRepository extends JpaRepository<RoutingHeader, String> {
    List<RoutingHeader> findByItemId(String itemId);
}
