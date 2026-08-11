package com.bemo.hr.trade.parties.application;

import com.bemo.hr.trade.parties.domain.BankChangeRequest;
import com.bemo.hr.trade.parties.infrastructure.BankChangeRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BankChangeGovernanceServiceTests {

    private BankChangeRequestRepository bankChangeRequestRepository;
    private BankChangeGovernanceService governanceService;

    @BeforeEach
    void setUp() {
        bankChangeRequestRepository = mock(BankChangeRequestRepository.class);
        governanceService = new BankChangeGovernanceService(bankChangeRequestRepository);
    }

    @Test
    void requestsApprovesAndRejectsBankChangeSuccessfully() {
        when(bankChangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BankChangeRequest req = governanceService.requestBankChange(
                BankChangeRequest.PartyType.SUPPLIER, "party-1", "EG001", "EG002", "Bank A", "Bank B", "Account Update", "user1"
        );
        assertThat(req).isNotNull();
        assertThat(req.getStatus()).isEqualTo(BankChangeRequest.Status.PENDING);

        when(bankChangeRequestRepository.findById("req-1")).thenReturn(Optional.of(req));

        governanceService.approveBankChange("req-1", "admin1");
        assertThat(req.getStatus()).isEqualTo(BankChangeRequest.Status.APPROVED);
        assertThat(req.getApprovedBy()).isEqualTo("admin1");
    }
}
