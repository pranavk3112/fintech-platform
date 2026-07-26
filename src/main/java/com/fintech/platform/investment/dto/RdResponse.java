package com.fintech.platform.investment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RdResponse {
    private Long id;
    private String rdNumber;
    private BigDecimal monthlyInstallment;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal totalDeposited;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private String status;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private LocalDate nextInstallmentDate;
    private Integer installmentsPaid;
    private Integer missedInstallments;
    private LocalDateTime createdAt;
}