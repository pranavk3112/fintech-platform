package com.fintech.platform.upi.controller;

import com.fintech.platform.common.response.ApiResponse;
import com.fintech.platform.upi.dto.*;
import com.fintech.platform.upi.service.UpiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/upi")
@RequiredArgsConstructor
public class UpiController {

    private final UpiService upiService;

    @PostMapping("/pin/set")
    public ResponseEntity<ApiResponse<Void>> setUpiPin(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SetUpiPinRequest request) {

        upiService.setUpiPin(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("UPI PIN set successfully", null));
    }

    @GetMapping("/validate/{upiId}")
    public ResponseEntity<ApiResponse<VpaValidationResponse>> validateVpa(
            @PathVariable String upiId) {

        VpaValidationResponse response = upiService.validateVpa(upiId);
        return ResponseEntity.ok(ApiResponse.success("VPA validation result", response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<UpiTransactionResponse>> upiTransfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpiTransferRequest request) {

        UpiTransactionResponse response = upiService
                .upiTransfer(userDetails.getUsername(), request);

        String message = response.getStatus().equals("COMPLETED")
                ? "UPI transfer successful"
                : "UPI transfer failed";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<UpiTransactionResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<UpiTransactionResponse> response = upiService
                .getUpiHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("UPI history fetched", response));
    }
}