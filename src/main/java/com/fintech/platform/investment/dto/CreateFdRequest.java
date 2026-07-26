package com.fintech.platform.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFdRequest {

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum FD amount is ₹1000")
    private BigDecimal principalAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 120, message = "Maximum tenure is 120 months (10 years)")
    private Integer tenureMonths;

    private String compoundingFrequency = "QUARTERLY";
}