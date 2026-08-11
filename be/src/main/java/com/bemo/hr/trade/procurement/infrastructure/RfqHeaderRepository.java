package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.RfqHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RfqHeaderRepository extends JpaRepository<RfqHeader, String> {
    List<RfqHeader> findByRequisitionId(String requisitionId);
}
