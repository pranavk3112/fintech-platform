package com.fintech.platform.wallet.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
import com.fintech.platform.wallet.dto.FundWalletRequest;
import com.fintech.platform.wallet.dto.WalletResponse;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    // ── Create wallet for new user ───────────────────────────────
    @Transactional
    public WalletResponse createWallet(String email) {
        log.info("Creating wallet for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            throw new BadRequestException("Wallet already exists for this user");
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Wallet created with id: {} for user: {}", saved.getId(), email);

        return mapToResponse(saved);
    }

    // ── Get wallet ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public WalletResponse getWallet(String email) {
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return mapToResponse(wallet);
    }

    // ── Fund wallet ──────────────────────────────────────────────
    @Transactional
    public WalletResponse fundWallet(String email, FundWalletRequest request) {
        log.info("Funding wallet for user: {} with amount: {}", email, request.getAmount());

        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        validateWalletActive(wallet);

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet updated = walletRepository.save(wallet);

        log.info("Wallet funded. New balance: {} for user: {}", updated.getBalance(), email);
        return mapToResponse(updated);
    }

    // ── Internal debit (used by transaction service later) ───────
    @Transactional
    public void debitWallet(Wallet wallet, BigDecimal amount) {
        validateWalletActive(wallet);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    // ── Internal credit (used by transaction service later) ──────
    @Transactional
    public void creditWallet(Wallet wallet, BigDecimal amount) {
        validateWalletActive(wallet);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    // ── Validation ───────────────────────────────────────────────
    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() != Wallet.WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active");
        }
    }

    // ── Mapper ───────────────────────────────────────────────────
    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .ownerEmail(wallet.getUser().getEmail())
                .ownerName(wallet.getUser().getFullName())
                .balance(wallet.getBalance())
                .status(wallet.getStatus().name())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}