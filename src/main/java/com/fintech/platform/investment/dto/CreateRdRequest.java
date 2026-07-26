package com.fintech.platform.investment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateRdRequest {

    @NotNull(message = "Monthly installment is required")
    @DecimalMin(value = "500.00", message = "Minimum RD installment is ₹500")
    private BigDecimal monthlyInstallment;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Minimum tenure is 6 months")
    @Max(value = 120, message = "Maximum tenure is 120 months")
    private Integer tenureMonths;
}