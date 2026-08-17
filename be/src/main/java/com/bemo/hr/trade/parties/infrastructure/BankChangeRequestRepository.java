package com.bemo.hr.trade.parties.infrastructure;

import com.bemo.hr.trade.parties.domain.BankChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankChangeRequestRepository extends JpaRepository<BankChangeRequest, String> {
    List<BankChangeRequest> findByPartyId(String partyId);

    List<BankChangeRequest> findByStatus(BankChangeRequest.Status status);
}
