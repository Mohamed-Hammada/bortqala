package com.bemo.hr.project.application;

import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.project.api.TenderApi.*;
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
class ProjectTenderServiceTests {

    @Mock
    private ProjectTenderRepository tenderRepository;

    @Mock
    private TenderBoqItemRepository boqItemRepository;

    @Mock
    private TenderBidderRepository bidderRepository;

    @Mock
    private BidSubmissionLineRepository submissionLineRepository;

    @Mock
    private TenderClarificationRepository clarificationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuditService auditService;

    private ProjectTenderService service;

    private Project project;
    private ProjectTender tender;
    private TenderBidder bidderA;
    private TenderBidder bidderB;

    @BeforeEach
    void setUp() {
        service = new ProjectTenderService(
                tenderRepository,
                boqItemRepository,
                bidderRepository,
                submissionLineRepository,
                clarificationRepository,
                projectRepository,
                auditService
        );

        project = new Project(
                "PRJ-101",
                "مستشفى الشروق المركزي",
                "Shorouk Hospital",
                "مشروع إنشاء مستشفى",
                "c-1",
                "b-1",
                "party-client",
                "pm-1",
                "الشروق",
                "CNT-555",
                BigDecimal.valueOf(100000000),
                "EGP",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2027, 4, 1),
                true
        );

        tender = new ProjectTender(
                "TND-2026-001",
                "مناقصة أعمال التشطيبات والكهروميكانيك",
                "MEP & Finishes Tender",
                TenderType.INTERNAL,
                project.getId(),
                "party-client",
                System.currentTimeMillis() + 86400000L * 15,
                BigDecimal.valueOf(25000000),
                "EGP",
                70,
                30,
                true,
                BigDecimal.valueOf(500000),
                90,
                "Internal Subcontractor Competition"
        );

        bidderA = new TenderBidder(tender.getId(), "party-sub-1", "شركة الأهرام للمقاولات", "info@ahram.com", "0101234567", null);
        bidderA.recordSubmission(BigDecimal.valueOf(20000000)); // 20M (Higher price)
        bidderA.recordEvaluation(BigDecimal.valueOf(90.0), null, null, 0); // Tech: 90/100

        bidderB = new TenderBidder(tender.getId(), "party-sub-2", "النيل للإنشاءات", "info@nile.com", "0109876543", null);
        bidderB.recordSubmission(BigDecimal.valueOf(16000000)); // 16M (Lowest price = 100 fin score)
        bidderB.recordEvaluation(BigDecimal.valueOf(80.0), null, null, 0); // Tech: 80/100
    }

    @Test
    void createTender_generatesSequentialNumberAndSaves() {
        when(tenderRepository.countByYearPrefix(anyString())).thenReturn(5L);
        when(tenderRepository.save(any(ProjectTender.class))).thenAnswer(i -> i.getArgument(0));
        when(tenderRepository.findById(anyString())).thenReturn(Optional.of(tender));

        CreateTenderRequest req = new CreateTenderRequest(
                "مناقصة أعمال الواجهات",
                "Facade Tender",
                TenderType.EXTERNAL,
                null,
                "party-client",
                System.currentTimeMillis() + 86400000L * 30,
                BigDecimal.valueOf(15000000),
                "EGP",
                60,
                40,
                true,
                BigDecimal.valueOf(300000),
                90,
                "Tender Notes"
        );

        ProjectTenderResponse res = service.createTender(req, "pm-1");

        assertThat(res).isNotNull();
        verify(tenderRepository).save(any(ProjectTender.class));
        verify(auditService).record(eq("TENDER_CREATE"), eq("PROJECT_TENDER"), any(), eq("pm-1"), any(), isNull());
    }

    @Test
    void publishTender_transitionsDraftToPublished() {
        when(tenderRepository.findById(tender.getId())).thenReturn(Optional.of(tender));
        when(tenderRepository.save(tender)).thenReturn(tender);

        ProjectTenderResponse res = service.publishTender(tender.getId(), "pm-1");

        assertThat(res.status()).isEqualTo(TenderStatus.PUBLISHED);
        verify(auditService).record(eq("TENDER_PUBLISH"), eq("PROJECT_TENDER"), eq(tender.getId()), eq("pm-1"), any(), isNull());
    }

    @Test
    void calculateTenderEvaluation_computesWeightedScoresAndRanks() {
        tender.publish();
        when(tenderRepository.findById(tender.getId())).thenReturn(Optional.of(tender));
        when(bidderRepository.findByTenderIdOrderByRankOrderAsc(tender.getId())).thenReturn(List.of(bidderA, bidderB));

        TenderEvaluationSummaryResponse summary = service.calculateTenderEvaluation(tender.getId(), "pm-1");

        assertThat(summary).isNotNull();
        assertThat(summary.lowestCompliantBidAmount()).isEqualTo(BigDecimal.valueOf(16000000));

        // Bidder B has lowest bid (16M) -> FinScore = 100.0, TechScore = 80.0
        // Combined Score = (80 * 0.7) + (100 * 0.3) = 56 + 30 = 86.0
        assertThat(bidderB.getFinancialScore()).isEqualTo(BigDecimal.valueOf(100.00).setScale(2));
        assertThat(bidderB.getCombinedScore()).isEqualTo(BigDecimal.valueOf(86.00).setScale(2));

        // Bidder A has 20M bid -> FinScore = (16M / 20M) * 100 = 80.0, TechScore = 90.0
        // Combined Score = (90 * 0.7) + (80 * 0.3) = 63 + 24 = 87.0
        assertThat(bidderA.getFinancialScore()).isEqualTo(BigDecimal.valueOf(80.00).setScale(2));
        assertThat(bidderA.getCombinedScore()).isEqualTo(BigDecimal.valueOf(87.00).setScale(2));

        // Bidder A ranks 1st because 87.0 > 86.0
        assertThat(bidderA.getRankOrder()).isEqualTo(1);
        assertThat(bidderB.getRankOrder()).isEqualTo(2);

        assertThat(tender.getStatus()).isEqualTo(TenderStatus.EVALUATION);
    }

    @Test
    void awardTender_updatesTenderAndSynchronizesProjectContract() {
        tender.publish();
        tender.startEvaluation();

        when(tenderRepository.findById(tender.getId())).thenReturn(Optional.of(tender));
        when(bidderRepository.findById(bidderB.getId())).thenReturn(Optional.of(bidderB));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(tenderRepository.save(tender)).thenReturn(tender);

        AwardTenderRequest req = new AwardTenderRequest(bidderB.getId(), "Awarded to lowest bidder", true);
        ProjectTenderResponse res = service.awardTender(tender.getId(), req, "pm-1");

        assertThat(res.status()).isEqualTo(TenderStatus.AWARDED);
        assertThat(tender.getAwardedBidderId()).isEqualTo(bidderB.getId());
        assertThat(tender.getAwardedAmount()).isEqualTo(BigDecimal.valueOf(16000000));

        // Verify project contract synchronization
        assertThat(project.getContractValue()).isEqualTo(BigDecimal.valueOf(16000000));
        verify(projectRepository).save(project);
    }
}
