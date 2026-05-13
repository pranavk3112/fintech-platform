package com.fintech.platform.upi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetUpiPinRequest {

    @NotBlank(message = "UPI PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "UPI PIN must be 4 or 6 digits")
    private String upiPin;
}