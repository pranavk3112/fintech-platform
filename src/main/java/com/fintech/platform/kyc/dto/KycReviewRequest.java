package com.fintech.platform.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycReviewRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Document ID is required")
    private Long documentId;

    @NotBlank(message = "Action is required")
    private String action;

    private String remarks;
}