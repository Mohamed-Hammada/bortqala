package com.bemo.hr.payroll.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EgyptianStatutoryPayrollServiceTests {

    private EgyptianStatutoryPayrollService service;

    @BeforeEach
    void setUp() {
        service = new EgyptianStatutoryPayrollService();
    }

    @Test
    void nullOrZeroGross_returnsAllZeroes() {
        var resNull = service.calculate(null);
        assertEquals(new BigDecimal("0.00"), resNull.monthlyGrossSalary());
        assertEquals(new BigDecimal("0.00"), resNull.monthlyNetSalary());
        assertTrue(resNull.taxBracketsBreakdown().isEmpty());

        var resZero = service.calculate(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), resZero.monthlyGrossSalary());
        assertEquals(new BigDecimal("0.00"), resZero.monthlyNetSalary());
        assertTrue(resZero.taxBracketsBreakdown().isEmpty());
    }

    @Test
    void lowIncome_underPersonalExemption_paysZeroIncomeTax() {
        // Gross 3,000 EGP / month
        // Insurable Wage = 3,000
        // Employee SI = 3,000 * 11% = 330 EGP
        // Employer SI = 3,000 * 18.75% = 562.50 EGP
        // Martyrs = 3,000 * 0.0005 = 1.50 EGP
        // Annual Gross = 36,000
        // Annual Employee SI = 330 * 12 = 3,960
        // Annual Taxable = 36,000 - 3,960 - 20,000 = 12,040 EGP (in bracket 1: 0..21,000 @ 0%)
        // Monthly Tax = 0.00
        var res = service.calculate(new BigDecimal("3000.00"));

        assertEquals(new BigDecimal("3000.00"), res.monthlyGrossSalary());
        assertEquals(new BigDecimal("3000.00"), res.monthlyInsurableWage());
        assertEquals(new BigDecimal("330.00"), res.monthlyEmployeeSocialInsurance());
        assertEquals(new BigDecimal("562.50"), res.monthlyEmployerSocialInsurance());
        assertEquals(new BigDecimal("1.50"), res.monthlyMartyrsFund());
        assertEquals(new BigDecimal("12040.00"), res.annualTaxableIncome());
        assertEquals(new BigDecimal("0.00"), res.annualIncomeTax());
        assertEquals(new BigDecimal("0.00"), res.monthlyIncomeTax());
        assertEquals(new BigDecimal("331.50"), res.totalEmployeeStatutoryDeductions());
        assertEquals(new BigDecimal("2668.50"), res.monthlyNetSalary());
    }

    @Test
    void midIncome_progressiveTaxBracketsCalculatedCorrectly() {
        // Gross 10,000 EGP / month
        // Insurable Wage = 10,000
        // Employee SI = 1,100 EGP
        // Employer SI = 1,875 EGP
        // Martyrs = 10,000 * 0.0005 = 5.00 EGP
        // Annual Gross = 120,000
        // Annual Employee SI = 1,100 * 12 = 13,200
        // Annual Taxable = 120,000 - 13,200 - 20,000 = 86,800 EGP
        // Tax Brackets on 86,800:
        // B1: 0..21,000 (21,000) @ 0% = 0
        // B2: 21,001..30,000 (9,000) @ 2.5% = 225.00
        // B3: 30,001..45,000 (15,000) @ 10% = 1,500.00
        // B4: 45,001..60,000 (15,000) @ 15% = 2,250.00
        // B5: 60,001..86,800 (26,800) @ 20% = 5,360.00
        // Total Annual Tax = 225 + 1500 + 2250 + 5360 = 9,335.00
        // Monthly Tax = 9,335 / 12 = 777.92
        var res = service.calculate(new BigDecimal("10000.00"));

        assertEquals(new BigDecimal("10000.00"), res.monthlyGrossSalary());
        assertEquals(new BigDecimal("1100.00"), res.monthlyEmployeeSocialInsurance());
        assertEquals(new BigDecimal("1875.00"), res.monthlyEmployerSocialInsurance());
        assertEquals(new BigDecimal("5.00"), res.monthlyMartyrsFund());
        assertEquals(new BigDecimal("86800.00"), res.annualTaxableIncome());
        assertEquals(new BigDecimal("9335.00"), res.annualIncomeTax());
        assertEquals(new BigDecimal("777.92"), res.monthlyIncomeTax());
        assertEquals(5, res.taxBracketsBreakdown().size());
    }

    @Test
    void highIncome_cappedInsurableWageAndUpperBrackets() {
        // Gross 50,000 EGP / month
        // Insurable Wage capped at MAX_INSURABLE_WAGE = 12,600 EGP
        // Employee SI = 12,600 * 11% = 1,386.00 EGP
        // Martyrs = 50,000 * 0.0005 = 25.00 EGP
        var res = service.calculate(new BigDecimal("50000.00"));

        assertEquals(new BigDecimal("50000.00"), res.monthlyGrossSalary());
        assertEquals(new BigDecimal("12600.00"), res.monthlyInsurableWage());
        assertEquals(new BigDecimal("1386.00"), res.monthlyEmployeeSocialInsurance());
        assertEquals(new BigDecimal("2362.50"), res.monthlyEmployerSocialInsurance());
        assertEquals(new BigDecimal("25.00"), res.monthlyMartyrsFund());
        assertTrue(res.taxBracketsBreakdown().size() >= 6);
        assertTrue(res.monthlyIncomeTax().compareTo(BigDecimal.ZERO) > 0);
    }
}
