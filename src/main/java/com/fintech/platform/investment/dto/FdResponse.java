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
public class FdResponse {
    private Long id;
    private String fdNumber;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private String interestType;
    private String compoundingFrequency;
    private BigDecimal maturityAmount;
    private BigDecimal interestEarned;
    private String status;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal prematureWithdrawalPenalty;
    private LocalDateTime createdAt;
}