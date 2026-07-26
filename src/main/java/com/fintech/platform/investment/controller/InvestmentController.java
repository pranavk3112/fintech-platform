package com.fintech.platform.investment.controller;

import com.fintech.platform.common.response.ApiResponse;
import com.fintech.platform.investment.dto.*;
import com.fintech.platform.investment.entity.RdInstallment;
import com.fintech.platform.investment.service.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    // ── FD Endpoints ─────────────────────────────────────────────
    @PostMapping("/fd")
    public ResponseEntity<ApiResponse<FdResponse>> createFd(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateFdRequest request) {

        FdResponse response = investmentService
                .createFd(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fixed Deposit created successfully", response));
    }

    @GetMapping("/fd")
    public ResponseEntity<ApiResponse<List<FdResponse>>> getUserFds(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FdResponse> response = investmentService
                .getUserFds(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("FDs fetched successfully", response));
    }

    @PostMapping("/fd/{id}/close")
    public ResponseEntity<ApiResponse<FdResponse>> closeFd(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        FdResponse response = investmentService
                .closeFd(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("FD closed successfully", response));
    }

    // ── RD Endpoints ─────────────────────────────────────────────
    @PostMapping("/rd")
    public ResponseEntity<ApiResponse<RdResponse>> createRd(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRdRequest request) {

        RdResponse response = investmentService
                .createRd(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recurring Deposit created successfully", response));
    }

    @GetMapping("/rd")
    public ResponseEntity<ApiResponse<List<RdResponse>>> getUserRds(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<RdResponse> response = investmentService
                .getUserRds(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("RDs fetched successfully", response));
    }

    @PostMapping("/rd/{id}/pay")
    public ResponseEntity<ApiResponse<RdResponse>> payInstallment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        RdResponse response = investmentService
                .payRdInstallment(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("RD installment paid successfully", response));
    }

    @GetMapping("/rd/{id}/installments")
    public ResponseEntity<ApiResponse<List<RdInstallmentResponse>>> getRdInstallments(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {

        List<RdInstallmentResponse> response = investmentService
                .getRdInstallments(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("RD installments fetched", response));
    }
}