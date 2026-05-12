package com.fintech.platform.wallet.controller;

import com.fintech.platform.common.response.ApiResponse;
import com.fintech.platform.wallet.dto.FundWalletRequest;
import com.fintech.platform.wallet.dto.WalletResponse;
import com.fintech.platform.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @AuthenticationPrincipal UserDetails userDetails) {

        WalletResponse response = walletService.createWallet(userDetails.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal UserDetails userDetails) {

        WalletResponse response = walletService.getWallet(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallet fetched successfully", response));
    }

    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<WalletResponse>> fundWallet(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FundWalletRequest request) {

        WalletResponse response = walletService.fundWallet(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Wallet funded successfully", response));
    }
}