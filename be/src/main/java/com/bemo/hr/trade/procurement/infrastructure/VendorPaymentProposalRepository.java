package com.bemo.hr.trade.procurement.infrastructure;

import com.bemo.hr.trade.procurement.domain.VendorPaymentProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VendorPaymentProposalRepository extends JpaRepository<VendorPaymentProposal, String> {
    List<VendorPaymentProposal> findBySupplierId(String supplierId);
}
