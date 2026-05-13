package com.fintech.platform.transaction.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.notification.event.TransferCompletedEvent;
import com.fintech.platform.transaction.dto.TransferRequest;
import com.fintech.platform.transaction.dto.TransactionResponse;
import com.fintech.platform.transaction.entity.Transaction;
import com.fintech.platform.transaction.repository.TransactionRepository;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import com.fintech.platform.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final TransactionRecorder transactionRecorder;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionResponse transfer(String senderEmail, TransferRequest request) {
        log.info("Transfer initiated by: {} amount: {} idempotencyKey: {}",
                senderEmail, request.getAmount(), request.getIdempotencyKey());

        // Step 1: Idempotency check
        Optional<Transaction> existing =
                transactionRepository.findCompletedByIdempotencyKey(request.getIdempotencyKey());

        if (existing.isPresent()) {
            log.info("Duplicate completed transaction for idempotencyKey: {}",
                    request.getIdempotencyKey());
            return mapToResponse(existing.get());
        }
        log.info("No completed transaction found for idempotencyKey: {}. Processing.",
                request.getIdempotencyKey());

        // Step 2: Load source wallet
        Wallet sourceWallet = walletRepository.findByUserEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Source wallet not found"));

        // Step 3: Load destination wallet
        Wallet destinationWallet = walletRepository.findByIdWithUser(request.getDestinationWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination wallet not found"));

        // Step 4: Cannot transfer to own wallet
        if (sourceWallet.getId().equals(destinationWallet.getId())) {
            throw new BadRequestException("Cannot transfer to your own wallet");
        }

        // Step 5: Save PENDING in its own committed transaction
        Transaction transaction = Transaction.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .sourceWallet(sourceWallet)
                .destinationWallet(destinationWallet)
                .amount(request.getAmount())
                .balanceBeforeSource(sourceWallet.getBalance())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        transaction = transactionRecorder.savePendingTransaction(transaction);

        // Step 6: Execute the transfer
        try {
            executeTransfer(sourceWallet, destinationWallet, request.getAmount());

            transactionRecorder.saveCompletedTransaction(
                    transaction.getId(),
                    sourceWallet.getBalance()
            );

            // Publish event for async notification
            eventPublisher.publishEvent(new TransferCompletedEvent(
                    this,
                    transaction.getId(),
                    senderEmail,
                    destinationWallet.getUser().getEmail(),
                    request.getAmount(),
                    sourceWallet.getBalance()
            ));

            log.info("Transfer completed. TransactionId: {}", transaction.getId());

        } catch (Exception e) {
            log.error("Transfer failed. TransactionId: {} Reason: {}",
                    transaction.getId(), e.getMessage());
            transactionRecorder.saveFailedTransaction(transaction.getId(), e.getMessage());
            throw e;
        }

        return mapToResponse(transactionRepository.findById(transaction.getId())
                .orElseThrow());
    }

    @Transactional
    public void executeTransfer(Wallet sourceWallet,
                                Wallet destinationWallet,
                                java.math.BigDecimal amount) {
        walletService.debitWallet(sourceWallet, amount);
        walletService.creditWallet(destinationWallet, amount);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(String email) {
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        return transactionRepository.findAllByWalletId(wallet.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId, String email) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        boolean isOwner = transaction.getSourceWallet() != null &&
                transaction.getSourceWallet().getId().equals(wallet.getId()) ||
                transaction.getDestinationWallet() != null &&
                        transaction.getDestinationWallet().getId().equals(wallet.getId());

        if (!isOwner) {
            throw new BadRequestException("Access denied to this transaction");
        }

        return mapToResponse(transaction);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .idempotencyKey(t.getIdempotencyKey())
                .sourceWalletId(t.getSourceWallet() != null
                        ? t.getSourceWallet().getId() : null)
                .destinationWalletId(t.getDestinationWallet() != null
                        ? t.getDestinationWallet().getId() : null)
                .amount(t.getAmount())
                .balanceBeforeSource(t.getBalanceBeforeSource())
                .balanceAfterSource(t.getBalanceAfterSource())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .build();
    }
}