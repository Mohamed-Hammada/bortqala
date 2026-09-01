package com.bemo.hr.payroll.api;

import com.bemo.hr.payroll.application.EgyptianStatutoryPayrollService;
import com.bemo.hr.payroll.application.PayrollCalculationPolicyService;
import com.bemo.hr.payroll.application.PayrollService;
import com.bemo.hr.payroll.application.PayrollWpsExportService;
import com.bemo.hr.payroll.domain.PaymentStatus;
import com.bemo.hr.shared.security.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollControllerTests {

    @Mock
    private PayrollService payrollService;
    @Mock
    private AuthService authService;
    @Mock
    private PayrollCalculationPolicyService payrollCalculationPolicyService;
    @Mock
    private EgyptianStatutoryPayrollService egyptianStatutoryPayrollService;
    @Mock
    private PayrollWpsExportService payrollWpsExportService;

    @InjectMocks
    private PayrollController controller;

    @BeforeEach
    void setUp() {
    }

    @Test
    void calculateStatutoryTax_returnsExpectedBreakdown() {
        var dummyResult = new EgyptianStatutoryPayrollService.StatutoryCalculationResult(
                new BigDecimal("10000.00"), new BigDecimal("10000.00"), new BigDecimal("1100.00"),
                new BigDecimal("1875.00"), new BigDecimal("5.00"), new BigDecimal("86800.00"),
                new BigDecimal("9335.00"), new BigDecimal("777.92"), new BigDecimal("1882.92"),
                new BigDecimal("8117.08"), List.of()
        );
        when(egyptianStatutoryPayrollService.calculate(new BigDecimal("10000.00"))).thenReturn(dummyResult);

        var response = controller.calculateStatutoryTax(new PayrollApi.StatutoryTaxRequest(new BigDecimal("10000.00")));
        assertNotNull(response);
        assertEquals(new BigDecimal("10000.00"), response.monthlyGrossSalary());
        assertEquals(new BigDecimal("1100.00"), response.monthlyEmployeeSocialInsurance());
        assertEquals(new BigDecimal("777.92"), response.monthlyIncomeTax());
    }

    @Test
    void exportWps_returnsByteResponseEntityWithAttachmentHeader() {
        var summary = new PayrollApi.Summary(1, 1, 0, new BigDecimal("10000.00"), new BigDecimal("9000.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        var sheet = new PayrollApi.SheetResponse(2026, 8, PaymentStatus.APPROVED, summary, List.of());
        when(payrollService.getSheet(2026, 8, null)).thenReturn(sheet);
        when(payrollWpsExportService.generateWpsFile(eq(sheet), eq(PayrollWpsExportService.WpsFormat.EG_WPS), any(), any()))
                .thenReturn("CSV_DATA".getBytes());

        var response = controller.exportWps(2026, 8, PayrollWpsExportService.WpsFormat.EG_WPS, "TAX-1", "CIB", null);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
