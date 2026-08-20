package com.bemo.hr.organization;

import com.bemo.hr.organization.api.OrganizationApi;
import com.bemo.hr.organization.application.IntercompanyService;
import com.bemo.hr.organization.domain.*;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.IntercompanyTransactionRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntercompanyServiceTests {

    @Mock
    private IntercompanyTransactionRepository transactionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    private IntercompanyService service;

    private Company companyA;
    private Company companyB;
    private Branch branchCairo;
    private Branch branchAlex;

    @BeforeEach
    void setUp() {
        service = new IntercompanyService(transactionRepository, companyRepository, branchRepository);

        companyA = new Company("CMP-01", "Al-Ahram Trading", "TX-1001", "CR-5001", true);
        companyB = new Company("CMP-02", "Bemo Contracting", "TX-1002", "CR-5002", true);

        branchCairo = new Branch(companyA.getId(), "BR-CAI", "Cairo Branch", "Nasr City", true);
        branchAlex = new Branch(companyB.getId(), "BR-ALX", "Alexandria Branch", "Smouha", true);
    }

    @Test
    @DisplayName("Create intercompany transaction generates sequential number and PENDING_APPROVAL status")
    void createTransaction_success() {
        when(companyRepository.findById(companyA.getId())).thenReturn(Optional.of(companyA));
        when(companyRepository.findById(companyB.getId())).thenReturn(Optional.of(companyB));
        when(transactionRepository.findLatestByNumberPrefix(anyString())).thenReturn(List.of());
        when(transactionRepository.save(any(IntercompanyTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationApi.CreateIntercompanyPayload payload = new OrganizationApi.CreateIntercompanyPayload(
                companyA.getId(),
                branchCairo.getId(),
                companyB.getId(),
                branchAlex.getId(),
                IntercompanyType.INVENTORY_TRANSFER,
                BigDecimal.valueOf(150_000.00),
                "EGP",
                "Raw materials transfer to Alex site",
                "ACC-DUE-TO",
                "ACC-DUE-FROM"
        );

        OrganizationApi.IntercompanyTransactionResponse response = service.createTransaction(payload);

        assertThat(response).isNotNull();
        assertThat(response.transactionNumber()).startsWith("IC-");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(150_000.00));
        assertThat(response.status()).isEqualTo(IntercompanyStatus.PENDING_APPROVAL);
        assertThat(response.transactionType()).isEqualTo(IntercompanyType.INVENTORY_TRANSFER);
    }

    @Test
    @DisplayName("Create transaction with same originating and destination company throws exception")
    void createTransaction_sameCompany_throwsException() {
        OrganizationApi.CreateIntercompanyPayload payload = new OrganizationApi.CreateIntercompanyPayload(
                companyA.getId(),
                null,
                companyA.getId(),
                null,
                IntercompanyType.MANAGEMENT_FEE,
                BigDecimal.valueOf(50_000.00),
                "EGP",
                "Internal fee",
                null,
                null
        );

        assertThatThrownBy(() -> service.createTransaction(payload))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("different");
    }

    @Test
    @DisplayName("Approve transaction transitions status to APPROVED")
    void approveTransaction_success() {
        IntercompanyTransaction tx = new IntercompanyTransaction(
                "IC-2026-001",
                companyA.getId(),
                null,
                companyB.getId(),
                null,
                IntercompanyType.EXPENSE_ALLOCATION,
                BigDecimal.valueOf(75_000.00),
                "EGP",
                "Shared cloud hosting expenses",
                null,
                null
        );

        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(IntercompanyTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyRepository.findById(companyA.getId())).thenReturn(Optional.of(companyA));
        when(companyRepository.findById(companyB.getId())).thenReturn(Optional.of(companyB));

        OrganizationApi.IntercompanyTransactionResponse response = service.approveTransaction(tx.getId());

        assertThat(response.status()).isEqualTo(IntercompanyStatus.APPROVED);
    }

    @Test
    @DisplayName("Settle transaction transitions status to SETTLED")
    void settleTransaction_success() {
        IntercompanyTransaction tx = new IntercompanyTransaction(
                "IC-2026-001",
                companyA.getId(),
                null,
                companyB.getId(),
                null,
                IntercompanyType.LOAN_ADVANCE,
                BigDecimal.valueOf(200_000.00),
                "EGP",
                "Current account bridge advance",
                null,
                null
        );
        tx.approve();

        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(IntercompanyTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(companyRepository.findById(companyA.getId())).thenReturn(Optional.of(companyA));
        when(companyRepository.findById(companyB.getId())).thenReturn(Optional.of(companyB));

        OrganizationApi.IntercompanyTransactionResponse response = service.settleTransaction(tx.getId());

        assertThat(response.status()).isEqualTo(IntercompanyStatus.SETTLED);
    }

    @Test
    @DisplayName("Run period elimination marks approved/settled transactions as ELIMINATED")
    void runPeriodElimination_success() {
        IntercompanyTransaction tx1 = new IntercompanyTransaction(
                "IC-2026-001",
                companyA.getId(),
                null,
                companyB.getId(),
                null,
                IntercompanyType.INVENTORY_TRANSFER,
                BigDecimal.valueOf(100_000.00),
                "EGP",
                "Transfer 1",
                null,
                null
        );
        tx1.approve();

        IntercompanyTransaction tx2 = new IntercompanyTransaction(
                "IC-2026-002",
                companyA.getId(),
                null,
                companyB.getId(),
                null,
                IntercompanyType.MANAGEMENT_FEE,
                BigDecimal.valueOf(50_000.00),
                "EGP",
                "Transfer 2",
                null,
                null
        );
        tx2.approve();
        tx2.settle();

        when(transactionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(tx1, tx2));

        OrganizationApi.EliminationResultResponse result = service.runPeriodElimination("2026-Q3");

        assertThat(result.eliminatedCount()).isEqualTo(2);
        assertThat(result.eliminatedTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150_000.00));
        assertThat(tx1.getStatus()).isEqualTo(IntercompanyStatus.ELIMINATED);
        assertThat(tx1.getEliminatedInPeriod()).isEqualTo("2026-Q3");
    }

    @Test
    @DisplayName("Consolidated summary aggregates metrics across branches and computes net margin")
    void getConsolidatedSummary_computesAggregations() {
        when(companyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(companyA, companyB));
        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(branchCairo, branchAlex));

        IntercompanyTransaction eliminatedTx = new IntercompanyTransaction(
                "IC-2026-001",
                companyA.getId(),
                null,
                companyB.getId(),
                null,
                IntercompanyType.INVENTORY_TRANSFER,
                BigDecimal.valueOf(80_000.00),
                "EGP",
                "Eliminated transfer",
                null,
                null
        );
        eliminatedTx.approve();
        eliminatedTx.eliminate("2026-Q3");

        when(transactionRepository.findByStatus(IntercompanyStatus.ELIMINATED)).thenReturn(List.of(eliminatedTx));

        OrganizationApi.ConsolidatedOrganizationSummary summary = service.getConsolidatedSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.activeBranches()).isEqualTo(2);
        assertThat(summary.totalRevenue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(summary.totalExpenses()).isGreaterThan(BigDecimal.ZERO);
        assertThat(summary.eliminatedTransfers()).isEqualByComparingTo(BigDecimal.valueOf(80_000.00));
        assertThat(summary.branchMetrics()).hasSize(2);
    }
}
