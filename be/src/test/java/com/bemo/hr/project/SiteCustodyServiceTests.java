package com.bemo.hr.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bemo.hr.project.api.ProjectApi.*;
import com.bemo.hr.project.application.SiteCustodyService;
import com.bemo.hr.project.domain.Project;
import com.bemo.hr.project.domain.ProjectStatus;
import com.bemo.hr.project.domain.SiteCustody;
import com.bemo.hr.project.domain.SiteCustodyExpense;
import com.bemo.hr.project.infrastructure.ProjectRepository;
import com.bemo.hr.project.infrastructure.SiteCustodyExpenseRepository;
import com.bemo.hr.project.infrastructure.SiteCustodyRepository;
import com.bemo.hr.project.infrastructure.SiteCustodyReturnRepository;
import com.bemo.hr.shared.domain.BusinessRuleException;
import com.bemo.hr.shared.security.TenantContext;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteCustodyServiceTests {

    @Mock
    private SiteCustodyRepository custodyRepository;

    @Mock
    private SiteCustodyExpenseRepository expenseRepository;

    @Mock
    private SiteCustodyReturnRepository returnRepository;

    @Mock
    private ProjectRepository projectRepository;

    private SiteCustodyService service;

    @BeforeEach
    void setUp() {
        TenantContext.set("app-test");
        service = new SiteCustodyService(custodyRepository, expenseRepository, returnRepository, projectRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void issueCustodySuccessfully() {
        Project project = new Project("SITE-01", "Site Construction", "Site Construction", null, null, null, null, null, null, null, BigDecimal.valueOf(500000), "EGP", null, null, false);
        when(projectRepository.findById("PRJ-01")).thenReturn(Optional.of(project));
        when(custodyRepository.save(any(SiteCustody.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseRepository.findByCustodyIdOrderByExpenseDateDesc(any())).thenReturn(Collections.emptyList());
        when(returnRepository.findByCustodyIdOrderByReturnDateDesc(any())).thenReturn(Collections.emptyList());

        IssueCustodyRequest req = new IssueCustodyRequest(
                "CUST-001",
                "EMP-01",
                "Eng. Karim Adel",
                "CASH",
                BigDecimal.valueOf(15000),
                "Site mobilization petty cash"
        );

        SiteCustodyResponse res = service.issueCustody("PRJ-01", req);
        assertThat(res.custodyCode()).isEqualTo("CUST-001");
        assertThat(res.custodianName()).isEqualTo("Eng. Karim Adel");
        assertThat(res.initialAmount()).isEqualByComparingTo("15000");
        assertThat(res.remainingBalance()).isEqualByComparingTo("15000");
        assertThat(res.status()).isEqualTo("ACTIVE");
    }

    @Test
    void recordExpenseSuccessfully() {
        SiteCustody custody = new SiteCustody("CUST-1", "app-test", "PRJ-01", "CUST-001", "EMP-01", "Eng. Karim Adel", "CASH", BigDecimal.valueOf(10000), System.currentTimeMillis(), "Notes");
        when(custodyRepository.findById("CUST-1")).thenReturn(Optional.of(custody));
        when(expenseRepository.save(any(SiteCustodyExpense.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordCustodyExpenseRequest req = new RecordCustodyExpenseRequest(
                System.currentTimeMillis(),
                BigDecimal.valueOf(2500),
                "FUEL",
                "Generator diesel fuel for site work",
                "REC-8821",
                "Site Foreman"
        );

        SiteCustodyExpenseResponse res = service.recordExpense("CUST-1", req);
        assertThat(res.amount()).isEqualByComparingTo("2500");
        assertThat(res.category()).isEqualTo("FUEL");
        assertThat(res.status()).isEqualTo("SUBMITTED");
    }

    @Test
    void recordExpenseExceedingBalanceThrows() {
        SiteCustody custody = new SiteCustody("CUST-1", "app-test", "PRJ-01", "CUST-001", "EMP-01", "Eng. Karim Adel", "CASH", BigDecimal.valueOf(1000), System.currentTimeMillis(), "Notes");
        when(custodyRepository.findById("CUST-1")).thenReturn(Optional.of(custody));

        RecordCustodyExpenseRequest req = new RecordCustodyExpenseRequest(
                System.currentTimeMillis(),
                BigDecimal.valueOf(5000),
                "MATERIALS",
                "Cement bags",
                "REC-11",
                "Site Foreman"
        );

        assertThatThrownBy(() -> service.recordExpense("CUST-1", req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds remaining custody balance");
    }

    @Test
    void approveExpenseDeductsBalance() {
        SiteCustody custody = new SiteCustody("CUST-1", "app-test", "PRJ-01", "CUST-001", "EMP-01", "Eng. Karim Adel", "CASH", BigDecimal.valueOf(10000), System.currentTimeMillis(), "Notes");
        SiteCustodyExpense expense = new SiteCustodyExpense("EXP-1", "app-test", "CUST-1", System.currentTimeMillis(), BigDecimal.valueOf(3000), "TOOLS", "Safety helmets and tools", "REC-99", "Purchasing");

        when(expenseRepository.findById("EXP-1")).thenReturn(Optional.of(expense));
        when(custodyRepository.findById("CUST-1")).thenReturn(Optional.of(custody));
        when(expenseRepository.save(any(SiteCustodyExpense.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteCustodyExpenseResponse res = service.approveExpense("EXP-1");
        assertThat(res.status()).isEqualTo("APPROVED");
        assertThat(custody.getRemainingBalance()).isEqualByComparingTo("7000");
    }

    @Test
    void settleCustodyWithReturn() {
        SiteCustody custody = new SiteCustody("CUST-1", "app-test", "PRJ-01", "CUST-001", "EMP-01", "Eng. Karim Adel", "CASH", BigDecimal.valueOf(10000), System.currentTimeMillis(), "Notes");
        custody.deductBalance(BigDecimal.valueOf(6000)); // 4000 remaining

        when(custodyRepository.findById("CUST-1")).thenReturn(Optional.of(custody));
        when(custodyRepository.save(any(SiteCustody.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseRepository.findByCustodyIdOrderByExpenseDateDesc(any())).thenReturn(Collections.emptyList());
        when(returnRepository.findByCustodyIdOrderByReturnDateDesc(any())).thenReturn(Collections.emptyList());

        SettleCustodyRequest req = new SettleCustodyRequest(
                BigDecimal.valueOf(4000),
                "Finance Dept Head",
                "Returned unused petty cash at project completion"
        );

        SiteCustodyResponse res = service.settleCustody("CUST-1", req);
        assertThat(res.status()).isEqualTo("SETTLED");
    }
}
