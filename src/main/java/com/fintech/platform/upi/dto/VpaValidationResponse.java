package com.fintech.platform.upi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpaValidationResponse {
    private String upiId;
    private String accountHolderName;
    private boolean valid;
}