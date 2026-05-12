package com.fintech.platform.transaction.controller;

import com.fintech.platform.common.response.ApiResponse;
import com.fintech.platform.transaction.dto.TransactionResponse;
import com.fintech.platform.transaction.dto.TransferRequest;
import com.fintech.platform.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {

        TransactionResponse response = transactionService
                .transfer(userDetails.getUsername(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer successful", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<TransactionResponse> response = transactionService
                .getTransactionHistory(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Transaction history fetched", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        TransactionResponse response = transactionService
                .getTransaction(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Transaction fetched", response));
    }
}