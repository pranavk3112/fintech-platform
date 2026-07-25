package com.fintech.platform.kyc.dto;

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
public class KycDocumentResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String documentType;
    private String documentNumber;
    private String documentUrl;
    private String verificationStatus;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}