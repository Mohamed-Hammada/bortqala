package com.bemo.hr.organization.application;

import com.bemo.hr.access.application.SecurityAuthorizationEvaluator;
import com.bemo.hr.employee.infrastructure.EmployeeRepository;
import com.bemo.hr.finance.infrastructure.BankAccountRepository;
import com.bemo.hr.finance.infrastructure.CashboxRepository;
import com.bemo.hr.operations.domain.StockTransferHeader;
import com.bemo.hr.operations.infrastructure.StockTransferHeaderRepository;
import com.bemo.hr.organization.api.OrganizationApi;
import com.bemo.hr.organization.domain.Branch;
import com.bemo.hr.organization.domain.Company;
import com.bemo.hr.organization.domain.Warehouse;
import com.bemo.hr.organization.infrastructure.BranchRepository;
import com.bemo.hr.organization.infrastructure.CompanyRepository;
import com.bemo.hr.organization.infrastructure.IntercompanyTransactionRepository;
import com.bemo.hr.organization.infrastructure.WarehouseRepository;
import com.bemo.hr.reporting.application.ExcelExportOptions;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.i18n.TranslationService;
import com.bemo.hr.trade.pos.infrastructure.PosTerminalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BranchControlCenterServiceTests {

    private BranchRepository branchRepository;
    private CompanyRepository companyRepository;
    private WarehouseRepository warehouseRepository;
    private IntercompanyTransactionRepository intercompanyRepository;
    private StockTransferHeaderRepository transferHeaderRepository;
    private CashboxRepository cashboxRepository;
    private BankAccountRepository bankAccountRepository;
    private PosTerminalRepository posTerminalRepository;
    private EmployeeRepository employeeRepository;
    private SecurityAuthorizationEvaluator authEvaluator;
    private TranslationService translationService;
    private BranchControlCenterService service;

    @BeforeEach
    void setUp() {
        branchRepository = mock(BranchRepository.class);
        companyRepository = mock(CompanyRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        intercompanyRepository = mock(IntercompanyTransactionRepository.class);
        transferHeaderRepository = mock(StockTransferHeaderRepository.class);
        cashboxRepository = mock(CashboxRepository.class);
        bankAccountRepository = mock(BankAccountRepository.class);
        posTerminalRepository = mock(PosTerminalRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        authEvaluator = mock(SecurityAuthorizationEvaluator.class);
        translationService = mock(TranslationService.class);

        service = new BranchControlCenterService(
                branchRepository,
                companyRepository,
                warehouseRepository,
                intercompanyRepository,
                transferHeaderRepository,
                cashboxRepository,
                bankAccountRepository,
                posTerminalRepository,
                employeeRepository,
                authEvaluator,
                translationService
        );
    }

    @Test
    void getPermittedBranches_filtersAccordingToEvaluator() {
        Branch b1 = new Branch("comp-1", "CAI", "Cairo Branch", "Cairo", true);
        Branch b2 = new Branch("comp-1", "ALX", "Alexandria Branch", "Alex", true);

        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(b1, b2));
        when(authEvaluator.hasBranchAccess(b1.getId())).thenReturn(true);
        when(authEvaluator.hasBranchAccess(b2.getId())).thenReturn(false);

        List<Branch> permitted = service.getPermittedBranches();

        assertThat(permitted).hasSize(1);
        assertThat(permitted.get(0).getCode()).isEqualTo("CAI");
    }

    @Test
    void getBranchControlSummary_calculatesSummaryAccurately() {
        Branch b1 = new Branch("comp-1", "CAI", "Cairo Branch", "Downtown", true,
                true, "+201000000000", "cairo@bemo.com", "123456789", "CR-101",
                "wh-1", "cb-1", "ba-1", "pos-1", "CAI");
        Company comp = new Company("BEMO-EG", "Bemo Egypt Ltd", "123456", "CR-01", true);

        Warehouse w1 = new Warehouse(b1.getId(), "WH-CAI-01", "Cairo Main WH", "Zone A", true);
        Warehouse w2 = new Warehouse(b1.getId(), "WH-CAI-02", "Cairo Secondary WH", "Zone B", true);

        StockTransferHeader transfer = new StockTransferHeader("TRF-001", w1.getId(), "wh-other", b1.getId(), "b-other", LocalDate.now());

        when(branchRepository.findById(b1.getId())).thenReturn(Optional.of(b1));
        when(authEvaluator.hasBranchAccess(b1.getId())).thenReturn(true);
        when(companyRepository.findById("comp-1")).thenReturn(Optional.of(comp));
        when(warehouseRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(w1, w2));
        when(transferHeaderRepository.findAll()).thenReturn(List.of(transfer));
        when(cashboxRepository.findAll()).thenReturn(List.of());
        when(bankAccountRepository.findAll()).thenReturn(List.of());
        when(posTerminalRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.findAll()).thenReturn(List.of());

        OrganizationApi.BranchControlSummary summary = service.getBranchControlSummary(b1.getId());

        assertThat(summary).isNotNull();
        assertThat(summary.branchCode()).isEqualTo("CAI");
        assertThat(summary.companyName()).isEqualTo("Bemo Egypt Ltd");
        assertThat(summary.isMainBranch()).isTrue();
        assertThat(summary.warehouseCount()).isEqualTo(2);
        assertThat(summary.activeTransfersCount()).isEqualTo(1);
    }

    @Test
    void getBranchControlSummary_accessDeniedThrowsForbidden() {
        Branch b1 = new Branch("comp-1", "GIZ", "Giza Branch", "Giza", true);
        when(branchRepository.findById(b1.getId())).thenReturn(Optional.of(b1));
        when(authEvaluator.hasBranchAccess(b1.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getBranchControlSummary(b1.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Branch access denied");
    }

    @Test
    void getConsolidatedGroupReport_filtersByCompanyAndBranch() {
        Company comp = new Company("BEMO-EG", "Bemo Egypt Ltd", "123", "456", true);
        Branch b1 = new Branch(comp.getId(), "CAI", "Cairo Branch", "Cairo", true);

        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(b1));
        when(authEvaluator.hasBranchAccess(b1.getId())).thenReturn(true);
        when(companyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(comp));
        when(companyRepository.findById(comp.getId())).thenReturn(Optional.of(comp));
        when(branchRepository.findById(b1.getId())).thenReturn(Optional.of(b1));
        when(intercompanyRepository.findByStatus(any())).thenReturn(List.of());

        OrganizationApi.ConsolidatedGroupReport report = service.getConsolidatedGroupReport(comp.getId(), b1.getId(), "2026-Q1");

        assertThat(report).isNotNull();
        assertThat(report.companyName()).isEqualTo("Bemo Egypt Ltd");
        assertThat(report.branchName()).isEqualTo("Cairo Branch");
        assertThat(report.totalRevenue()).isPositive();
        assertThat(report.grossProfit()).isPositive();
        assertThat(report.plLines()).isNotEmpty();
        assertThat(report.balanceSheetLines()).isNotEmpty();
    }

    @Test
    void exportConsolidatedReport_generatesValidExcelBytes() {
        Company comp = new Company("BEMO-EG", "Bemo Egypt Ltd", "123", "456", true);
        Branch b1 = new Branch(comp.getId(), "CAI", "Cairo Branch", "Cairo", true);

        when(branchRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(b1));
        when(authEvaluator.hasBranchAccess(b1.getId())).thenReturn(true);
        when(companyRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(comp));
        when(intercompanyRepository.findByStatus(any())).thenReturn(List.of());

        ExcelExportOptions options = new ExcelExportOptions("ar-EG", null);
        byte[] bytes = service.exportConsolidatedReport(comp.getId(), null, "2026-Q1", options);

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(100);
    }
}
