package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VendorPaymentProposalRepository extends JpaRepository<VendorPaymentProposal, String> {
    List<VendorPaymentProposal> findBySupplierId(String supplierId);
    List<VendorPaymentProposal> findAllByOrderByCreatedAtDesc();
    Optional<VendorPaymentProposal> findByOperationId(String operationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from VendorPaymentProposal p where p.id = :id")
    Optional<VendorPaymentProposal> findByIdForUpdate(@Param("id") String id);
}
