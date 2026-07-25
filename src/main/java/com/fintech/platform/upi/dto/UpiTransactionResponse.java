package com.fintech.platform.upi.dto;

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
public class UpiTransactionResponse {
    private Long id;
    private String senderUpiId;
    private String receiverUpiId;
    private BigDecimal amount;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
    private String failureReason;
}