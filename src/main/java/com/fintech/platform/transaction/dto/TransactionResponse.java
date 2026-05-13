package com.fintech.platform.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {
    private Long id;
    private String idempotencyKey;
    private Long sourceWalletId;
    private Long destinationWalletId;
    private BigDecimal amount;
    private BigDecimal balanceBeforeSource;
    private BigDecimal balanceAfterSource;
    private String type;
    private String status;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
}