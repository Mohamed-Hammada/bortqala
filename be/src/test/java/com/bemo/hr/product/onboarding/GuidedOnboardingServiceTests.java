package com.bemo.hr.product.onboarding;

import com.bemo.hr.attendance.infrastructure.ImportBatchRepository;
import com.bemo.hr.audit.application.AuditService;
import com.bemo.hr.employee.infrastructure.AttendanceCategoryRepository;
import com.bemo.hr.operations.InventoryItemRepository;
import com.bemo.hr.operations.infrastructure.StockStatusBalanceRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.party.BusinessParty;
import com.bemo.hr.party.BusinessPartyRepository;
import com.bemo.hr.product.onboarding.provider.CommonCompanyEvidenceProvider;
import com.bemo.hr.product.onboarding.provider.FoodDistributionOnboardingEvidenceProvider;
import com.bemo.hr.product.onboarding.provider.WorkforceOnboardingEvidenceProvider;
import com.bemo.hr.product.pack.*;
import com.bemo.hr.shared.security.TenantApplication;
import com.bemo.hr.shared.security.TenantApplicationRepository;
import com.bemo.hr.shared.security.TenantContext;
import com.bemo.hr.trade.procurement.infrastructure.GoodsReceiptRepository;
import com.bemo.hr.trade.procurement.infrastructure.PurchaseOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerCreditProfileRepository;
import com.bemo.hr.trade.sales.infrastructure.CustomerInvoiceRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesOrderRepository;
import com.bemo.hr.trade.sales.infrastructure.SalesPricingSnapshotRepository;
import com.bemo.hr.workforce.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuidedOnboardingServiceTests {
    @Mock
    IndustryPackRepository packRepository;
    @Mock
    TenantIndustryPackRepository tenantPackRepository;
    @Mock
    IndustryOnboardingStepRepository stepRepository;
    @Mock
    OnboardingAssessmentRepository assessmentRepository;
    @Mock
    TenantApplicationRepository applicationRepository;
    @Mock
    ContractorRepository contractorRepository;
    @Mock
    AttendanceCategoryRepository categoryRepository;
    @Mock
    WorkerRepository workerRepository;
    @Mock
    WorkforceImportBatchRepository workforceImportRepository;
    @Mock
    ImportBatchRepository attendanceImportRepository;
    @Mock
    WorkforceAdvanceRepository advanceRepository;
    @Mock
    ContractorSettlementRepository settlementRepository;
    @Mock
    WarehouseRepository warehouseRepository;
    @Mock
    InventoryItemRepository itemRepository;
    @Mock
    BusinessPartyRepository partyRepository;
    @Mock
    StockStatusBalanceRepository stockBalanceRepository;
    @Mock
    PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    GoodsReceiptRepository goodsReceiptRepository;
    @Mock
    SalesOrderRepository salesOrderRepository;
    @Mock
    CustomerInvoiceRepository customerInvoiceRepository;
    @Mock
    SalesPricingSnapshotRepository pricingSnapshotRepository;
    @Mock
    CustomerCreditProfileRepository creditProfileRepository;
    @Mock
    AuditService auditService;

    GuidedOnboardingService service;
    IndustryPack definition;
    TenantIndustryPack installed;
    List<IndustryOnboardingStep> steps;

    private static List<IndustryOnboardingStep> steps(String pack) {
        String[] keys = {"industryPack.step.company", "industryPack.step.contractors", "industryPack.step.categories", "industryPack.step.workers", "industryPack.step.attendance", "industryPack.step.advances", "industryPack.step.settlement"};
        List<IndustryOnboardingStep> result = new ArrayList<>();
        String previous = null;
        for (int i = 0; i < keys.length; i++) {
            result.add(new IndustryOnboardingStep(pack, keys[i], i + 1, previous, i == 5));
            previous = keys[i];
        }
        return result;
    }

    @BeforeEach
    void setup() {
        TenantContext.set("app-1");
        definition = new IndustryPack("pack", "CONTRACTOR_WORKFORCE_EG", 1, "[]");
        installed = new TenantIndustryPack(definition, "install", "admin", "{}");
        steps = steps(installed.getId());

        var companyProvider = new CommonCompanyEvidenceProvider(applicationRepository);
        var workforceProvider = new WorkforceOnboardingEvidenceProvider(
                contractorRepository, categoryRepository, workerRepository,
                workforceImportRepository, attendanceImportRepository, advanceRepository, settlementRepository
        );
        var foodProvider = new FoodDistributionOnboardingEvidenceProvider(
                warehouseRepository, itemRepository, partyRepository, stockBalanceRepository,
                purchaseOrderRepository, goodsReceiptRepository, salesOrderRepository,
                customerInvoiceRepository, pricingSnapshotRepository, creditProfileRepository
        );
        var evidenceRegistry = new OnboardingEvidenceRegistry(List.of(companyProvider, workforceProvider, foodProvider));
        var readinessService = new IndustryReadinessService(evidenceRegistry);

        service = new GuidedOnboardingService(
                packRepository, tenantPackRepository, stepRepository, assessmentRepository,
                evidenceRegistry, readinessService, new ObjectMapper(), auditService
        );

        lenient().when(packRepository.findByCodeAndStatus(definition.getCode(), "ACTIVE")).thenReturn(Optional.of(definition));
        lenient().when(tenantPackRepository.findByPackId(definition.getId())).thenReturn(Optional.of(installed));
        lenient().when(tenantPackRepository.findByPackIdForUpdate(definition.getId())).thenReturn(Optional.of(installed));
        lenient().when(stepRepository.findByTenantPackIdOrderBySequenceNo(installed.getId())).thenReturn(steps);
        lenient().when(stepRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(assessmentRepository.findByTenantPackIdAndOperationId(anyString(), anyString())).thenReturn(Optional.empty());
        lenient().when(assessmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void overviewContainsOnlySelectedVerticalStepsAndActionableIssues() {
        var result = service.overview(definition.getCode());
        assertThat(result.steps()).extracting(GuidedOnboardingApi.StepResponse::key)
                .containsExactly("industryPack.step.company", "industryPack.step.contractors", "industryPack.step.categories", "industryPack.step.workers", "industryPack.step.attendance", "industryPack.step.advances", "industryPack.step.settlement");
        assertThat(result.issues()).extracting(GuidedOnboardingApi.IssueResponse::route)
                .contains("/settings", "/workforce/contractors", "/imports");
        assertThat(result.dataQualityScore()).isZero();
    }

    @Test
    void successfulImportAutomaticallyCompletesAttendanceAfterDependencies() {
        readyBusinessData();
        when(advanceRepository.count()).thenReturn(0L);
        when(settlementRepository.count()).thenReturn(0L);
        var result = service.assess(definition.getCode(), new GuidedOnboardingApi.AssessRequest("assess-1"), "admin");
        assertThat(result.steps().stream().filter(s -> s.key().equals("industryPack.step.attendance")).findFirst().orElseThrow().status()).isEqualTo("COMPLETED");
        assertThat(result.steps().stream().filter(s -> s.key().equals("industryPack.step.advances")).findFirst().orElseThrow().status()).isEqualTo("READY");
        assertThat(result.steps().stream().filter(s -> s.key().equals("industryPack.step.settlement")).findFirst().orElseThrow().status()).isEqualTo("BLOCKED");
    }

    @Test
    void optionalSkipUnlocksFinalStepAndReadyRequiresQuality() {
        readyBusinessData();
        when(settlementRepository.count()).thenReturn(1L);
        service.assess(definition.getCode(), new GuidedOnboardingApi.AssessRequest("first"), "admin");
        steps.get(5).complete("admin", true);
        steps.get(6).ready();
        var result = service.assess(definition.getCode(), new GuidedOnboardingApi.AssessRequest("second"), "admin");
        assertThat(result.setupProgress()).isEqualTo(100);
        assertThat(result.dataQualityScore()).isEqualTo(100);
        assertThat(result.readiness()).isEqualTo("READY");
        assertThat(result.steps().get(5).status()).isEqualTo("SKIPPED");
    }

    @Test
    void operationReplayDoesNotReassessOrDuplicateAudit() {
        var snapshot = new OnboardingAssessment(installed.getId(), "same", 80, 80, "IN_PROGRESS", "[]", "admin");
        when(assessmentRepository.findByTenantPackIdAndOperationId(installed.getId(), "same")).thenReturn(Optional.of(snapshot));
        var result = service.assess(definition.getCode(), new GuidedOnboardingApi.AssessRequest("same"), "admin");
        assertThat(result.dataQualityScore()).isEqualTo(80);
        verify(assessmentRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void foodDistributionOnboardingEvaluatesDistributionEvidence() {
        IndustryPack food = new IndustryPack("food", "FOOD_DISTRIBUTION_EG", "food.name", "food.desc", 1,
                "[]", "{}", "[\"SALES_MANAGER\"]", "[\"fillRate\"]", "[\"items.xlsx\"]",
                "[\"industryPack.food.step.company\",\"industryPack.food.step.warehouses\",\"industryPack.food.step.items\"]");
        TenantIndustryPack foodInstalled = new TenantIndustryPack(food, "food-inst", "admin", "{}");

        List<IndustryOnboardingStep> foodSteps = List.of(
                new IndustryOnboardingStep(foodInstalled.getId(), "industryPack.food.step.company", 1, null, false),
                new IndustryOnboardingStep(foodInstalled.getId(), "industryPack.food.step.warehouses", 2, "industryPack.food.step.company", false),
                new IndustryOnboardingStep(foodInstalled.getId(), "industryPack.food.step.items", 3, "industryPack.food.step.warehouses", false)
        );

        lenient().when(packRepository.findByCodeAndStatus("FOOD_DISTRIBUTION_EG", "ACTIVE")).thenReturn(Optional.of(food));
        lenient().when(tenantPackRepository.findByPackId(food.getId())).thenReturn(Optional.of(foodInstalled));
        lenient().when(tenantPackRepository.findByPackIdForUpdate(food.getId())).thenReturn(Optional.of(foodInstalled));
        lenient().when(stepRepository.findByTenantPackIdOrderBySequenceNo(foodInstalled.getId())).thenReturn(foodSteps);

        TenantApplication app = new TenantApplication("BEMO", "Bemo Distribution");
        lenient().when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        lenient().when(warehouseRepository.count()).thenReturn(2L);
        lenient().when(itemRepository.count()).thenReturn(10L);

        var result = service.assess("FOOD_DISTRIBUTION_EG", new GuidedOnboardingApi.AssessRequest("food-assess"), "admin");
        assertThat(result.packCode()).isEqualTo("FOOD_DISTRIBUTION_EG");
        assertThat(result.steps()).hasSize(3);
        assertThat(result.steps().stream().allMatch(s -> "COMPLETED".equals(s.status()))).isTrue();
        assertThat(result.readiness()).isEqualTo("READY");
    }

    private void readyBusinessData() {
        TenantApplication app = new TenantApplication("BEMO", "Bemo Corp");
        lenient().when(applicationRepository.findById("app-1")).thenReturn(Optional.of(app));
        lenient().when(contractorRepository.count()).thenReturn(1L);
        lenient().when(categoryRepository.count()).thenReturn(1L);
        lenient().when(workerRepository.count()).thenReturn(1L);
        lenient().when(workforceImportRepository.existsByStatus("IMPORTED")).thenReturn(true);
    }

}
