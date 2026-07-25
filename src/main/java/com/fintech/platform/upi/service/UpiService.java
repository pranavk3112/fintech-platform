package com.fintech.platform.upi.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.transaction.dto.TransactionResponse;
import com.fintech.platform.transaction.dto.TransferRequest;
import com.fintech.platform.transaction.service.TransactionService;
import com.fintech.platform.upi.dto.*;
import com.fintech.platform.upi.entity.UpiTransaction;
import com.fintech.platform.upi.repository.UpiTransactionRepository;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpiService {

    private final WalletRepository walletRepository;
    private final UpiTransactionRepository upiTransactionRepository;
    private final TransactionService transactionService;
    private final PasswordEncoder passwordEncoder;
    private final UpiTransactionRecorder upiTransactionRecorder;

    // ── Set UPI PIN ──────────────────────────────────────────────
    @Transactional
    public void setUpiPin(String email, SetUpiPinRequest request) {
        log.info("Setting UPI PIN for user: {}", email);

        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        wallet.setUpiPinHash(passwordEncoder.encode(request.getUpiPin()));
        wallet.setUpiPinSet(true);
        walletRepository.save(wallet);

        log.info("UPI PIN set successfully for user: {}", email);
    }

    // ── Validate VPA ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public VpaValidationResponse validateVpa(String upiId) {
        log.info("Validating VPA: {}", upiId);

        return walletRepository.findByUpiId(upiId)
                .map(wallet -> VpaValidationResponse.builder()
                        .upiId(upiId)
                        .accountHolderName(wallet.getUser().getFullName())
                        .valid(true)
                        .build())
                .orElse(VpaValidationResponse.builder()
                        .upiId(upiId)
                        .valid(false)
                        .build());
    }

    // ── UPI Transfer ─────────────────────────────────────────────
    public UpiTransactionResponse upiTransfer(String senderEmail,
                                              UpiTransferRequest request) {
        log.info("UPI transfer initiated by: {} to UPI: {}",
                senderEmail, request.getReceiverUpiId());

        // Step 1: Load sender wallet
        Wallet senderWallet = walletRepository.findByUserEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        // Step 2: Check UPI PIN is set
        if (!senderWallet.getUpiPinSet()) {
            throw new BadRequestException("UPI PIN not set. Please set your UPI PIN first.");
        }

        // Step 3: Validate UPI PIN
        if (!passwordEncoder.matches(request.getUpiPin(), senderWallet.getUpiPinHash())) {
            throw new BadRequestException("Invalid UPI PIN");
        }

        // Step 4: Validate receiver VPA
        Wallet receiverWallet = walletRepository.findByUpiId(request.getReceiverUpiId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invalid UPI ID: " + request.getReceiverUpiId()));

        // Step 5: Cannot send to own UPI ID
        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new BadRequestException("Cannot transfer to your own UPI ID");
        }

        // Step 6: Check UPI-level idempotency
        Optional<UpiTransaction> existingUpi =
                upiTransactionRepository.findCompletedByIdempotencyKey(request.getIdempotencyKey());

        if (existingUpi.isPresent()) {
            log.info("Duplicate completed UPI request for idempotencyKey: {}",
                    request.getIdempotencyKey());
            return mapToResponse(existingUpi.get());
        }
        log.info("Processing UPI transfer for idempotencyKey: {}",
                request.getIdempotencyKey());

        // Step 7: Execute transfer via TransactionService
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setAmount(request.getAmount());
        transferRequest.setDestinationWalletId(receiverWallet.getId());
        transferRequest.setIdempotencyKey(request.getIdempotencyKey());
        transferRequest.setDescription(request.getRemarks());

        String status;
        String failureReason = null;

        try {
            TransactionResponse transactionResponse = transactionService
                    .transfer(senderEmail, transferRequest);
            status = transactionResponse.getStatus();
        } catch (Exception e) {
            status = "FAILED";
            failureReason = e.getMessage();
            log.error("UPI transfer failed for idempotencyKey: {} Reason: {}",
                    request.getIdempotencyKey(), e.getMessage());
        }

        // Step 8: Always save UPI transaction record in independent transaction
        UpiTransaction upiTransaction = UpiTransaction.builder()
                .senderUpiId(senderWallet.getUpiId())
                .receiverUpiId(request.getReceiverUpiId())
                .amount(request.getAmount())
                .status(status)
                .remarks(request.getRemarks())
                .idempotencyKey(request.getIdempotencyKey())
                .failureReason(failureReason)
                .build();

        UpiTransaction saved = upiTransactionRecorder.save(upiTransaction);

        if ("FAILED".equals(status)) {
            throw new BadRequestException(failureReason != null
                    ? failureReason : "UPI transfer failed");
        }

        return mapToResponse(saved);
    }

    // ── UPI Transaction History ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<UpiTransactionResponse> getUpiHistory(String email) {
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        List<UpiTransaction> sent = upiTransactionRepository
                .findBySenderUpiIdOrderByCreatedAtDesc(wallet.getUpiId());
        List<UpiTransaction> received = upiTransactionRepository
                .findByReceiverUpiIdOrderByCreatedAtDesc(wallet.getUpiId());

        sent.addAll(received);
        return sent.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Mapper ───────────────────────────────────────────────────
    private UpiTransactionResponse mapToResponse(UpiTransaction t) {
        return UpiTransactionResponse.builder()
                .id(t.getId())
                .senderUpiId(t.getSenderUpiId())
                .receiverUpiId(t.getReceiverUpiId())
                .amount(t.getAmount())
                .status(t.getStatus())
                .remarks(t.getRemarks())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .build();
    }
}