package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.project.api.ClaimApi.*;
import com.bemo.hr.project.domain.*;
import com.bemo.hr.project.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectProgressClaimServiceTests {

    @Mock
    private ProjectProgressClaimRepository claimRepository;

    @Mock
    private ProgressClaimLineRepository lineRepository;

    @Mock
    private ProgressClaimAdjustmentRepository adjustmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WbsNodeRepository wbsNodeRepository;

    @Mock
    private BusinessPartyRepository partyRepository;

    @Mock
    private AuditService auditService;

    private ProjectProgressClaimService service;

    private Project project;
    private ProjectProgressClaim claim;
    private ProgressClaimLine line1;
    private ProgressClaimAdjustment retentionAdj;

    @BeforeEach
    void setUp() {
        service = new ProjectProgressClaimService(
                claimRepository,
                lineRepository,
                adjustmentRepository,
                projectRepository,
                wbsNodeRepository,
                partyRepository,
                auditService
        );

        project = new Project(
                "PRJ-202",
                "أبراج العاصمة الإدارية",
                "Capital Towers",
                "مشروع سكني تجاري",
                "c-1",
                "b-1",
                "party-owner",
                "pm-1",
                "العاصمة الإدارية",
                "CNT-888",
                BigDecimal.valueOf(50000000),
                "EGP",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                true
        );

        claim = new ProjectProgressClaim(
                "IPC-OWN-2026-001",
                ClaimType.OWNER_IPC,
                ClaimKind.INTERIM,
                1,
                project.getId(),
                "party-owner",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "EGP",
                "Monthly progress claim #1"
        );

        line1 = new ProgressClaimLine(
                claim.getId(),
                ClaimLineType.BOQ_ITEM,
                "wbs-1",
                "01.01",
                "أعمال الحفر والردم",
                "M3",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(150),
                BigDecimal.ZERO,
                BigDecimal.valueOf(400), // 400 M3 * 150 = 60,000
                "Foundation excavation",
                1
        );

        retentionAdj = new ProgressClaimAdjustment(
                claim.getId(),
                AdjustmentType.RETENTION,
                "تأمين أعمال (5%)",
                BigDecimal.valueOf(5.0),
                BigDecimal.valueOf(60000),
                BigDecimal.ZERO,
                false,
                null
        );
    }

    @Test
    void createClaim_withWbsInitialization() {
        WbsNode boqNode = new WbsNode(
                project.getId(),
                null,
                "01.01",
                "/01.01",
                "أعمال الخرسانات المسلحة",
                "RC Concrete",
                "خرسانة مسلحة",
                WbsNodeType.BOQ_ITEM,
                1,
                1,
                "M3",
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(2000),
                "cc-1",
                null,
                null,
                WbsNodeStatus.PLANNED
        );

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(claimRepository.findByProjectIdAndClaimTypeOrderByClaimSequenceNumberDesc(project.getId(), ClaimType.OWNER_IPC))
                .thenReturn(List.of());
        when(claimRepository.save(any(ProjectProgressClaim.class))).thenAnswer(i -> i.getArgument(0));
        when(claimRepository.findById(anyString())).thenReturn(Optional.of(claim));
        when(wbsNodeRepository.findByProjectIdOrderBySortOrderAsc(project.getId())).thenReturn(List.of(boqNode));
        when(lineRepository.findByClaimIdOrderBySortOrderAsc(anyString())).thenReturn(List.of(line1));
        when(adjustmentRepository.findByClaimId(anyString())).thenReturn(List.of(retentionAdj));

        CreateProgressClaimRequest req = new CreateProgressClaimRequest(
                ClaimType.OWNER_IPC,
                ClaimKind.INTERIM,
                project.getId(),
                "party-owner",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "EGP",
                "June Claim",
                true
        );

        ProjectProgressClaimResponse res = service.createClaim(req, "pm-1");

        assertThat(res).isNotNull();
        verify(lineRepository, atLeastOnce()).save(any(ProgressClaimLine.class));
        verify(adjustmentRepository, atLeastOnce()).save(any(ProgressClaimAdjustment.class));
        verify(auditService).record(eq("CLAIM_CREATE"), eq("PROJECT_PROGRESS_CLAIM"), any(), eq("pm-1"), any(), isNull());
    }

    @Test
    void updateDraftClaim_recalculatesCumulativeAndAdjustments() {
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(any(ProjectProgressClaim.class))).thenAnswer(i -> i.getArgument(0));
        when(lineRepository.findByClaimIdOrderBySortOrderAsc(claim.getId())).thenReturn(List.of(line1));
        when(adjustmentRepository.findByClaimId(claim.getId())).thenReturn(List.of(retentionAdj));

        SaveClaimLineRequest lineReq = new SaveClaimLineRequest(
                null,
                ClaimLineType.BOQ_ITEM,
                "wbs-1",
                "01.01",
                "أعمال الحفر والردم",
                "M3",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(150),
                BigDecimal.ZERO,
                BigDecimal.valueOf(400),
                null,
                1
        );

        SaveClaimAdjustmentRequest adjReq = new SaveClaimAdjustmentRequest(
                null,
                AdjustmentType.RETENTION,
                "تأمين أعمال (5%)",
                BigDecimal.valueOf(5.0),
                null,
                false,
                null
        );

        UpdateProgressClaimRequest req = new UpdateProgressClaimRequest(
                ClaimKind.INTERIM,
                "party-owner",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "EGP",
                "Updated",
                List.of(lineReq),
                List.of(adjReq)
        );

        ProjectProgressClaimResponse res = service.updateDraftClaim(claim.getId(), req, "pm-1");

        assertThat(res).isNotNull();

        // 400 * 150 = 60,000 Gross
        // 5% Retention = 3,000
        // Net Payable = 60,000 - 3,000 = 57,000
        assertThat(claim.getCurrentGrossAmount()).isEqualTo(BigDecimal.valueOf(60000).setScale(2));
        assertThat(claim.getCurrentRetentionAmount()).isEqualTo(BigDecimal.valueOf(3000.00).setScale(2));
        assertThat(claim.getCurrentNetPayableAmount()).isEqualTo(BigDecimal.valueOf(57000.00).setScale(2));
    }

    @Test
    void certifyClaim_locksCertification() {
        claim.submit();
        claim.review();

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(claim)).thenReturn(claim);

        CertifyClaimRequest req = new CertifyClaimRequest("Certified by Consultant");
        ProjectProgressClaimResponse res = service.certifyClaim(claim.getId(), req, "eng-1");

        assertThat(res.status()).isEqualTo(ClaimStatus.CERTIFIED);
        assertThat(claim.getCertifiedByUserId()).isEqualTo("eng-1");
        assertThat(claim.getCertificationNotes()).isEqualTo("Certified by Consultant");
    }

    @Test
    void postClaimToFinance_generatesJournalAndInvoice() {
        claim.submit();
        claim.review();
        claim.certify("eng-1", "OK");

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(claimRepository.save(claim)).thenReturn(claim);

        ProjectProgressClaimResponse res = service.postClaimToFinance(claim.getId(), "fin-1");

        assertThat(res.status()).isEqualTo(ClaimStatus.POSTED_FINANCE);
        assertThat(claim.getPostedFinanceJournalId()).isNotNull();
        assertThat(claim.getPostedInvoiceId()).isEqualTo("INV-" + claim.getClaimNumber());
    }
}
