package com.fintech.platform.kyc.controller;

import com.fintech.platform.common.response.ApiResponse;
import com.fintech.platform.kyc.dto.*;
import com.fintech.platform.kyc.entity.KycAuditLog;
import com.fintech.platform.kyc.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    // Customer endpoints
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> submitDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody KycSubmissionRequest request) {

        KycDocumentResponse response = kycService
                .submitDocument(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("KYC document submitted successfully", response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<KycStatusResponse>> getKycStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        KycStatusResponse response = kycService.getKycStatus(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("KYC status fetched", response));
    }

    // Admin endpoints
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getPendingDocuments() {

        List<KycDocumentResponse> response = kycService.getPendingDocuments();
        return ResponseEntity.ok(ApiResponse.success("Pending KYC documents fetched", response));
    }

    @PostMapping("/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> reviewDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody KycReviewRequest request) {

        KycDocumentResponse response = kycService
                .reviewDocument(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("KYC review completed", response));
    }

    @GetMapping("/audit/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KycAuditLog>>> getAuditLog(
            @PathVariable Long userId) {

        List<KycAuditLog> response = kycService.getAuditLog(userId);
        return ResponseEntity.ok(ApiResponse.success("KYC audit log fetched", response));
    }
}