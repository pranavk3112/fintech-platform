package com.fintech.platform.beneficiary.controller;

import com.fintech.platform.beneficiary.dto.AddBeneficiaryRequest;
import com.fintech.platform.beneficiary.dto.BeneficiaryResponse;
import com.fintech.platform.beneficiary.dto.BeneficiaryTransferRequest;
import com.fintech.platform.beneficiary.service.BeneficiaryService;
import com.fintech.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddBeneficiaryRequest request) {

        BeneficiaryResponse response = beneficiaryService
                .addBeneficiary(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Beneficiary added successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> listBeneficiaries(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<BeneficiaryResponse> response = beneficiaryService
                .listBeneficiaries(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Beneficiaries fetched", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        beneficiaryService.deleteBeneficiary(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Beneficiary deleted", null));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Object>> transferToBeneficiary(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BeneficiaryTransferRequest request) {

        Object response = beneficiaryService
                .transferToBeneficiary(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Transfer initiated", response));
    }
}