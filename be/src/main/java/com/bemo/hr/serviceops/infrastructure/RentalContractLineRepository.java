package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.RentalContractLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RentalContractLineRepository extends JpaRepository<RentalContractLine, String> {
    List<RentalContractLine> findByAppIdAndContractId(String appId, String contractId);
}
