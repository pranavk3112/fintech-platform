package com.fintech.platform.investment.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class InterestCalculator {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    // ── FD Compound Interest ─────────────────────────────────────
    // A = P × (1 + r/n)^(n×t)
    public BigDecimal calculateFdMaturityAmount(BigDecimal principal,
                                                BigDecimal annualRate,
                                                int tenureMonths,
                                                int compoundingFrequency) {
        // r = annual rate / 100
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(100), MC);

        // t = tenure in years
        BigDecimal t = BigDecimal.valueOf(tenureMonths)
                .divide(BigDecimal.valueOf(12), MC);

        // n = compounding frequency per year
        BigDecimal n = BigDecimal.valueOf(compoundingFrequency);

        // (1 + r/n)
        BigDecimal base = BigDecimal.ONE.add(r.divide(n, MC));

        // (n × t)
        BigDecimal exponent = n.multiply(t);

        // base^exponent
        double baseDouble = base.doubleValue();
        double exponentDouble = exponent.doubleValue();
        double result = Math.pow(baseDouble, exponentDouble);

        BigDecimal maturityAmount = principal.multiply(
                BigDecimal.valueOf(result), MC);

        return maturityAmount.setScale(4, RoundingMode.HALF_UP);
    }

    // ── RD Maturity Amount ───────────────────────────────────────
    // M = R × [(1+i)^n - 1] / (1-(1+i)^(-1/3))
    // Simplified formula used by most Indian banks
    public BigDecimal calculateRdMaturityAmount(BigDecimal monthlyInstallment,
                                                BigDecimal annualRate,
                                                int tenureMonths) {
        // Quarterly rate
        double i = annualRate.doubleValue() / 400;

        // Number of quarters
        int n = tenureMonths / 3;

        double maturity = 0;

        // Sum for each installment
        // Each monthly installment earns interest for remaining quarters
        for (int month = 1; month <= tenureMonths; month++) {
            double remainingMonths = tenureMonths - month + 1;
            double quarters = remainingMonths / 3.0;
            double installmentMaturity = monthlyInstallment.doubleValue()
                    * Math.pow(1 + i, quarters);
            maturity += installmentMaturity;
        }

        return BigDecimal.valueOf(maturity).setScale(4, RoundingMode.HALF_UP);
    }

    // ── Get compounding frequency as integer ─────────────────────
    public int getCompoundingFrequency(String frequency) {
        return switch (frequency.toUpperCase()) {
            case "MONTHLY" -> 12;
            case "QUARTERLY" -> 4;
            case "HALF_YEARLY" -> 2;
            case "YEARLY" -> 1;
            default -> 4;
        };
    }
}