package com.fintech.platform.wallet.dto;

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
public class WalletResponse {
    private Long id;
    private String ownerEmail;
    private String ownerName;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;
}