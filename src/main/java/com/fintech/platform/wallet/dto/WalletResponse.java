package com.fintech.platform.wallet.dto;

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
public class WalletResponse {
    private Long id;
    private String ownerEmail;
    private String ownerName;
    private BigDecimal balance;
    private String status;
    private String walletType;
    private String upiId;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private String currency;
    private LocalDateTime createdAt;
}