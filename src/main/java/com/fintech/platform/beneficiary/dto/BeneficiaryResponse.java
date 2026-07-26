package com.fintech.platform.beneficiary.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BeneficiaryResponse {
    private Long id;
    private String nickname;
    private String beneficiaryType;
    private String upiId;
    private Long walletId;
    private String beneficiaryName;
    private LocalDateTime createdAt;
}