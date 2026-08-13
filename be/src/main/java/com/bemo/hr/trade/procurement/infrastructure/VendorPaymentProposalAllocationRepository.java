package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.VendorPaymentProposalAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPaymentProposalAllocationRepository extends JpaRepository<VendorPaymentProposalAllocation, String> {
    List<VendorPaymentProposalAllocation> findByProposalIdOrderByLineNoAsc(String proposalId);
}
