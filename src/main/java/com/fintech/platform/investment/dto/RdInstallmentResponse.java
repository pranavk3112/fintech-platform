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
public class RdInstallmentResponse {
    private Long id;
    private Integer installmentNumber;
    private BigDecimal amount;
    private String status;
    private LocalDate dueDate;
    private LocalDateTime paidDate;
    private BigDecimal penaltyAmount;
    private LocalDateTime createdAt;
}