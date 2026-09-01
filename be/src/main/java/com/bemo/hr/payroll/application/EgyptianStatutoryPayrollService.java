package com.bemo.hr.payroll.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EgyptianStatutoryPayrollService {

    // Law 30 of 2023 personal annual exemption
    public static final BigDecimal ANNUAL_PERSONAL_EXEMPTION = new BigDecimal("20000");

    // Social insurance rates (Law 148 of 2019)
    public static final BigDecimal SOCIAL_INSURANCE_EMPLOYEE_RATE = new BigDecimal("0.11"); // 11%
    public static final BigDecimal SOCIAL_INSURANCE_EMPLOYER_RATE = new BigDecimal("0.1875"); // 18.75%
    public static final BigDecimal MIN_INSURABLE_WAGE = new BigDecimal("2000.00");
    public static final BigDecimal MAX_INSURABLE_WAGE = new BigDecimal("12600.00");

    // Martyrs' Families & Disabilities Support Fund
    public static final BigDecimal MARTYRS_FUND_RATE = new BigDecimal("0.0005"); // 0.05%

    public record TaxBracketDetail(
            int bracketNumber,
            String bracketRange,
            BigDecimal ratePercent,
            BigDecimal taxableAmountInBracket,
            BigDecimal computedTax
    ) {
    }

    public record StatutoryCalculationResult(
            BigDecimal monthlyGrossSalary,
            BigDecimal monthlyInsurableWage,
            BigDecimal monthlyEmployeeSocialInsurance,
            BigDecimal monthlyEmployerSocialInsurance,
            BigDecimal monthlyMartyrsFund,
            BigDecimal annualTaxableIncome,
            BigDecimal annualIncomeTax,
            BigDecimal monthlyIncomeTax,
            BigDecimal totalEmployeeStatutoryDeductions,
            BigDecimal monthlyNetSalary,
            List<TaxBracketDetail> taxBracketsBreakdown
    ) {
    }

    public StatutoryCalculationResult calculate(BigDecimal grossSalary) {
        if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return new StatutoryCalculationResult(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    List.of()
            );
        }

        BigDecimal monthlyGross = grossSalary.setScale(2, RoundingMode.HALF_UP);

        // 1. Social Insurance
        BigDecimal insurableWage = monthlyGross.max(MIN_INSURABLE_WAGE).min(MAX_INSURABLE_WAGE);
        BigDecimal monthlyEmployeeSi = insurableWage.multiply(SOCIAL_INSURANCE_EMPLOYEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyEmployerSi = insurableWage.multiply(SOCIAL_INSURANCE_EMPLOYER_RATE).setScale(2, RoundingMode.HALF_UP);

        // 2. Martyrs Fund
        BigDecimal monthlyMartyrs = monthlyGross.multiply(MARTYRS_FUND_RATE).setScale(2, RoundingMode.HALF_UP);

        // 3. Income Tax (Law 30/2023)
        BigDecimal annualGross = monthlyGross.multiply(BigDecimal.valueOf(12));
        BigDecimal annualEmployeeSi = monthlyEmployeeSi.multiply(BigDecimal.valueOf(12));
        BigDecimal annualTaxable = annualGross.subtract(annualEmployeeSi).subtract(ANNUAL_PERSONAL_EXEMPTION).max(BigDecimal.ZERO);

        List<TaxBracketDetail> breakdown = new ArrayList<>();
        BigDecimal annualTax = calculateAnnualTax(annualTaxable, breakdown);
        BigDecimal monthlyTax = annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        // 4. Totals & Net
        BigDecimal totalDeductions = monthlyEmployeeSi.add(monthlyMartyrs).add(monthlyTax);
        BigDecimal monthlyNet = monthlyGross.subtract(totalDeductions).max(BigDecimal.ZERO);

        return new StatutoryCalculationResult(
                monthlyGross,
                insurableWage,
                monthlyEmployeeSi,
                monthlyEmployerSi,
                monthlyMartyrs,
                annualTaxable.setScale(2, RoundingMode.HALF_UP),
                annualTax.setScale(2, RoundingMode.HALF_UP),
                monthlyTax,
                totalDeductions,
                monthlyNet,
                breakdown
        );
    }

    private BigDecimal calculateAnnualTax(BigDecimal annualTaxable, List<TaxBracketDetail> breakdown) {
        if (annualTaxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal remaining = annualTaxable;
        BigDecimal totalTax = BigDecimal.ZERO;

        // Bracket 1: 0..21000 @ 0%
        BigDecimal b1Cap = new BigDecimal("21000");
        BigDecimal b1Taxable = remaining.min(b1Cap);
        breakdown.add(new TaxBracketDetail(1, "0 - 21,000", BigDecimal.ZERO, b1Taxable, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
        remaining = remaining.subtract(b1Taxable);

        // Bracket 2: 21001..30000 (9000 @ 2.5%)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal b2Cap = new BigDecimal("9000");
            BigDecimal b2Taxable = remaining.min(b2Cap);
            BigDecimal b2Tax = b2Taxable.multiply(new BigDecimal("0.025")).setScale(2, RoundingMode.HALF_UP);
            breakdown.add(new TaxBracketDetail(2, "21,001 - 30,000", new BigDecimal("2.5"), b2Taxable, b2Tax));
            totalTax = totalTax.add(b2Tax);
            remaining = remaining.subtract(b2Taxable);
        }

        // Bracket 3: 30001..45000 (15000 @ 10%)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal b3Cap = new BigDecimal("15000");
            BigDecimal b3Taxable = remaining.min(b3Cap);
            BigDecimal b3Tax = b3Taxable.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            breakdown.add(new TaxBracketDetail(3, "30,001 - 45,000", new BigDecimal("10.0"), b3Taxable, b3Tax));
            totalTax = totalTax.add(b3Tax);
            remaining = remaining.subtract(b3Taxable);
        }

        // Bracket 4: 45001..60000 (15000 @ 15%)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal b4Cap = new BigDecimal("15000");
            BigDecimal b4Taxable = remaining.min(b4Cap);
            BigDecimal b4Tax = b4Taxable.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
            breakdown.add(new TaxBracketDetail(4, "45,001 - 60,000", new BigDecimal("15.0"), b4Taxable, b4Tax));
            totalTax = totalTax.add(b4Tax);
            remaining = remaining.subtract(b4Taxable);
        }

        // Bracket 5: 60001..200000 (140000 @ 20%)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal b5Cap = new BigDecimal("140000");
            BigDecimal b5Taxable = remaining.min(b5Cap);
            BigDecimal b5Tax = b5Taxable.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP);
            breakdown.add(new TaxBracketDetail(5, "60,001 - 200,000", new BigDecimal("20.0"), b5Taxable, b5Tax));
            totalTax = totalTax.add(b5Tax);
            remaining = remaining.subtract(b5Taxable);
        }

        // Bracket 6: 200001..400000 (200000 @ 22.5%)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal b6Cap = new BigDecimal("200000");
            BigDecimal b6Taxable = remaining.min(b6Cap);
            BigDecimal b6Tax = b6Taxable.multiply(new BigDecimal("0.225")).setScale(2, RoundingMode.HALF_UP);
            breakdown.add(new TaxBracketDetail(6, "200,001 - 400,000", new BigDecimal("22.5"), b6Taxable, b6Tax));
            totalTax = totalTax.add(b6Tax);
            remaining = remaining.subtract(b6Taxable);
        }

        // Bracket 7: > 400000 (@ 25%)
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal b7Taxable = remaining;
            BigDecimal b7Tax = b7Taxable.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
            breakdown.add(new TaxBracketDetail(7, "Above 400,000", new BigDecimal("25.0"), b7Taxable, b7Tax));
            totalTax = totalTax.add(b7Tax);
        }

        return totalTax;
    }
}
