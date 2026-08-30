package com.bemo.hr.serviceops.infrastructure;

import com.bemo.hr.serviceops.domain.RentalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, String> {
    List<RentalContract> findByAppIdOrderByCreatedAtDesc(String appId);
    List<RentalContract> findByAppIdAndStatus(String appId, RentalContract.Status status);
    List<RentalContract> findByAppIdAndCustomerPartyId(String appId, String customerPartyId);
    Optional<RentalContract> findByAppIdAndId(String appId, String id);
    Optional<RentalContract> findByAppIdAndContractNo(String appId, String contractNo);
    long countByAppId(String appId);
    long countByAppIdAndStatus(String appId, RentalContract.Status status);
}
