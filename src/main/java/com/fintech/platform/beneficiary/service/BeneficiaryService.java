package com.fintech.platform.beneficiary.service;

import com.fintech.platform.beneficiary.dto.AddBeneficiaryRequest;
import com.fintech.platform.beneficiary.dto.BeneficiaryResponse;
import com.fintech.platform.beneficiary.dto.BeneficiaryTransferRequest;
import com.fintech.platform.beneficiary.entity.Beneficiary;
import com.fintech.platform.beneficiary.repository.BeneficiaryRepository;
import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.transaction.dto.TransactionResponse;
import com.fintech.platform.transaction.dto.TransferRequest;
import com.fintech.platform.transaction.service.TransactionService;
import com.fintech.platform.upi.dto.UpiTransferRequest;
import com.fintech.platform.upi.dto.UpiTransactionResponse;
import com.fintech.platform.upi.service.UpiService;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final UpiService upiService;

    // ── Add Beneficiary ──────────────────────────────────────────
    @Transactional
    public BeneficiaryResponse addBeneficiary(String email,
                                              AddBeneficiaryRequest request) {
        log.info("Adding beneficiary for user: {} nickname: {}", email, request.getNickname());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check duplicate nickname
        if (beneficiaryRepository.existsByUserIdAndNicknameIgnoreCase(
                user.getId(), request.getNickname())) {
            throw new BadRequestException(
                    "Beneficiary with nickname '" + request.getNickname() + "' already exists");
        }

        // Parse beneficiary type
        Beneficiary.BeneficiaryType type;
        try {
            type = Beneficiary.BeneficiaryType.valueOf(
                    request.getBeneficiaryType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid beneficiary type. Use UPI or WALLET");
        }

        Wallet beneficiaryWallet = null;
        String upiId = null;

        if (type == Beneficiary.BeneficiaryType.UPI) {
            if (request.getUpiId() == null || request.getUpiId().isBlank()) {
                throw new BadRequestException("UPI ID is required for UPI beneficiary");
            }
            // Validate UPI ID exists
            walletRepository.findByUpiId(request.getUpiId())
                    .orElseThrow(() -> new BadRequestException(
                            "Invalid UPI ID: " + request.getUpiId()));
            upiId = request.getUpiId();

        } else if (type == Beneficiary.BeneficiaryType.WALLET) {
            if (request.getWalletId() == null) {
                throw new BadRequestException("Wallet ID is required for WALLET beneficiary");
            }
            beneficiaryWallet = walletRepository.findById(request.getWalletId())
                    .orElseThrow(() -> new BadRequestException(
                            "Invalid wallet ID: " + request.getWalletId()));
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .user(user)
                .nickname(request.getNickname())
                .beneficiaryType(type)
                .upiId(upiId)
                .wallet(beneficiaryWallet)
                .beneficiaryName(request.getBeneficiaryName())
                .isActive(true)
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary added. Id: {} for user: {}", saved.getId(), email);

        return mapToResponse(saved);
    }

    // ── List Beneficiaries ───────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listBeneficiaries(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return beneficiaryRepository
                .findByUserIdAndIsActiveTrueOrderByNicknameAsc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Delete Beneficiary ───────────────────────────────────────
    @Transactional
    public void deleteBeneficiary(String email, Long beneficiaryId) {
        log.info("Deleting beneficiary: {} for user: {}", beneficiaryId, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Beneficiary beneficiary = beneficiaryRepository
                .findByUserIdAndId(user.getId(), beneficiaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));

        // Soft delete — never hard delete financial records
        beneficiary.setIsActive(false);
        beneficiaryRepository.save(beneficiary);

        log.info("Beneficiary soft deleted. Id: {}", beneficiaryId);
    }

    // ── Transfer to Beneficiary ──────────────────────────────────
    @Transactional
    public Object transferToBeneficiary(String senderEmail,
                                        BeneficiaryTransferRequest request) {
        log.info("Transfer to beneficiary: {} by user: {}",
                request.getNickname(), senderEmail);

        User user = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Beneficiary beneficiary = beneficiaryRepository
                .findByUserIdAndNicknameIgnoreCase(user.getId(), request.getNickname())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Beneficiary '" + request.getNickname() + "' not found"));

        if (!beneficiary.getIsActive()) {
            throw new BadRequestException("Beneficiary is inactive");
        }

        if (beneficiary.getBeneficiaryType() == Beneficiary.BeneficiaryType.UPI) {
            // UPI transfer
            if (request.getUpiPin() == null || request.getUpiPin().isBlank()) {
                throw new BadRequestException("UPI PIN is required for UPI transfer");
            }

            UpiTransferRequest upiRequest = new UpiTransferRequest();
            upiRequest.setReceiverUpiId(beneficiary.getUpiId());
            upiRequest.setAmount(request.getAmount());
            upiRequest.setUpiPin(request.getUpiPin());
            upiRequest.setIdempotencyKey(request.getIdempotencyKey());
            upiRequest.setRemarks(request.getRemarks() != null
                    ? request.getRemarks()
                    : "Transfer to " + beneficiary.getNickname());

            return upiService.upiTransfer(senderEmail, upiRequest);

        } else {
            // Wallet transfer
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setDestinationWalletId(beneficiary.getWallet().getId());
            transferRequest.setAmount(request.getAmount());
            transferRequest.setIdempotencyKey(request.getIdempotencyKey());
            transferRequest.setDescription(request.getRemarks() != null
                    ? request.getRemarks()
                    : "Transfer to " + beneficiary.getNickname());

            return transactionService.transfer(senderEmail, transferRequest);
        }
    }

    // ── Mapper ───────────────────────────────────────────────────
    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .nickname(b.getNickname())
                .beneficiaryType(b.getBeneficiaryType().name())
                .upiId(b.getUpiId())
                .walletId(b.getWallet() != null ? b.getWallet().getId() : null)
                .beneficiaryName(b.getBeneficiaryName())
                .createdAt(b.getCreatedAt())
                .build();
    }
}