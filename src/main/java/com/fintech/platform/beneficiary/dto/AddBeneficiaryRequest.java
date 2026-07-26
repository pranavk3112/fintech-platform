package com.fintech.platform.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBeneficiaryRequest {

    @NotBlank(message = "Nickname is required")
    private String nickname;

    @NotNull(message = "Beneficiary type is required")
    private String beneficiaryType;

    private String upiId;

    private Long walletId;

    @NotBlank(message = "Beneficiary name is required")
    private String beneficiaryName;
}