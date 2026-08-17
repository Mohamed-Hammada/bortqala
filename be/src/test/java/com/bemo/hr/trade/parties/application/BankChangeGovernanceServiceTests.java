package com.bemo.hr.trade.parties.application;

import com.bemo.hr.approval.SegregationOfDutiesService;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
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
    private BusinessPartyRepository businessPartyRepository;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        bankChangeRequestRepository = mock(BankChangeRequestRepository.class);
        businessPartyRepository = mock(BusinessPartyRepository.class);
        auditService = mock(AuditService.class);
        governanceService = new BankChangeGovernanceService(bankChangeRequestRepository, businessPartyRepository,
                new SegregationOfDutiesService(), auditService);
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
        BusinessParty party = mock(BusinessParty.class);
        when(party.getId()).thenReturn("party-1");
        when(businessPartyRepository.findById("party-1")).thenReturn(Optional.of(party));

        governanceService.approveBankChange("req-1", "admin1");
        assertThat(req.getStatus()).isEqualTo(BankChangeRequest.Status.APPROVED);
        assertThat(req.getApprovedBy()).isEqualTo("admin1");
        verify(party).verifyBank(eq("EG002"), eq("admin1"), any());
        verify(businessPartyRepository).save(party);
        verify(auditService).record(eq("BANK_CHANGE_APPROVED"), eq("BUSINESS_PARTY"), eq("party-1"), eq("admin1"), anyString(), isNull());
    }

    @Test
    void rejectsDirectApiSelfApproval() {
        BankChangeRequest req = new BankChangeRequest(BankChangeRequest.PartyType.SUPPLIER, "party-1",
                "EG001", "EG002", "Bank A", "Bank B", "Update", "maker");
        when(bankChangeRequestRepository.findById("req-1")).thenReturn(Optional.of(req));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> governanceService.approveBankChange("req-1", "maker"))
                .isInstanceOf(com.bemo.hr.approval.SegregationOfDutiesViolationException.class);
        verifyNoInteractions(businessPartyRepository);
    }
}
