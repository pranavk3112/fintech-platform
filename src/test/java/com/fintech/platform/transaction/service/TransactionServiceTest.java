package com.fintech.platform.transaction.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.transaction.dto.TransactionResponse;
import com.fintech.platform.transaction.dto.TransferRequest;
import com.fintech.platform.transaction.entity.Transaction;
import com.fintech.platform.transaction.repository.TransactionRepository;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import com.fintech.platform.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private TransactionRecorder transactionRecorder;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransactionService transactionService;

    private User senderUser;
    private User receiverUser;
    private Wallet sourceWallet;
    private Wallet destinationWallet;
    private TransferRequest transferRequest;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .fullName("Sender User")
                .email("sender@fintech.com")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        receiverUser = User.builder()
                .id(2L)
                .fullName("Receiver User")
                .email("receiver@fintech.com")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        sourceWallet = Wallet.builder()
                .id(1L)
                .user(senderUser)
                .balance(new BigDecimal("5000.00"))
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        destinationWallet = Wallet.builder()
                .id(2L)
                .user(receiverUser)
                .balance(new BigDecimal("1000.00"))
                .status(Wallet.WalletStatus.ACTIVE)
                .build();

        transferRequest = new TransferRequest();
        transferRequest.setAmount(new BigDecimal("500.00"));
        transferRequest.setDestinationWalletId(2L);
        transferRequest.setIdempotencyKey("test-key-001");
        transferRequest.setDescription("Test transfer");

        mockTransaction = Transaction.builder()
                .id(1L)
                .idempotencyKey("test-key-001")
                .sourceWallet(sourceWallet)
                .destinationWallet(destinationWallet)
                .amount(new BigDecimal("500.00"))
                .balanceBeforeSource(new BigDecimal("5000.00"))
                .balanceAfterSource(new BigDecimal("4500.00"))
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Test transfer")
                .build();
    }

    // ── Transfer Tests ───────────────────────────────────────────

    @Test
    @DisplayName("Should complete transfer successfully")
    void shouldCompleteTransferSuccessfully() {
        // Arrange
        when(transactionRepository.findCompletedByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithUser(anyLong()))
                .thenReturn(Optional.of(destinationWallet));
        when(transactionRecorder.savePendingTransaction(any()))
                .thenReturn(mockTransaction);
        when(transactionRepository.findById(anyLong()))
                .thenReturn(Optional.of(mockTransaction));

        // Act
        TransactionResponse response = transactionService
                .transfer("sender@fintech.com", transferRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAmount())
                .isEqualByComparingTo(new BigDecimal("500.00"));

        verify(transactionRecorder).savePendingTransaction(any());
        verify(walletService).debitWallet(eq(sourceWallet), eq(new BigDecimal("500.00")));
        verify(walletService).creditWallet(eq(destinationWallet), eq(new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("Should return existing transaction on duplicate idempotency key")
    void shouldReturnExistingTransactionOnDuplicateKey() {
        // Arrange
        when(transactionRepository.findCompletedByIdempotencyKey(anyString()))
                .thenReturn(Optional.of(mockTransaction));

        // Act
        TransactionResponse response = transactionService
                .transfer("sender@fintech.com", transferRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getIdempotencyKey()).isEqualTo("test-key-001");

        // Verify no wallet operations happened
        verify(walletRepository, never()).findByUserEmail(anyString());
        verify(walletService, never()).debitWallet(any(), any());
        verify(walletService, never()).creditWallet(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when transferring to own wallet")
    void shouldThrowExceptionWhenTransferringToOwnWallet() {
        // Arrange
        transferRequest.setDestinationWalletId(1L);

        when(transactionRepository.findCompletedByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithUser(anyLong()))
                .thenReturn(Optional.of(sourceWallet)); // ← sourceWallet not destinationWallet

        // Act & Assert
        assertThatThrownBy(() ->
                transactionService.transfer("sender@fintech.com", transferRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot transfer to your own wallet");

        verify(transactionRecorder, never()).savePendingTransaction(any());
    }

    @Test
    @DisplayName("Should record failed transaction on insufficient balance")
    void shouldRecordFailedTransactionOnInsufficientBalance() {
        // Arrange
        when(transactionRepository.findCompletedByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithUser(anyLong()))
                .thenReturn(Optional.of(destinationWallet));
        when(transactionRecorder.savePendingTransaction(any()))
                .thenReturn(mockTransaction);

        doThrow(new BadRequestException("Insufficient balance"))
                .when(walletService).debitWallet(any(), any());

        // Act & Assert
        assertThatThrownBy(() ->
                transactionService.transfer("sender@fintech.com", transferRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient balance");

        // Verify failure was recorded
        verify(transactionRecorder).saveFailedTransaction(
                eq(mockTransaction.getId()), eq("Insufficient balance"));
    }

    // ── Transaction History Tests ────────────────────────────────

    @Test
    @DisplayName("Should return transaction history for wallet")
    void shouldReturnTransactionHistory() {
        // Arrange
        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(sourceWallet));
        when(transactionRepository.findAllByWalletId(anyLong()))
                .thenReturn(List.of(mockTransaction));

        // Act
        List<TransactionResponse> history = transactionService
                .getTransactionHistory("sender@fintech.com");

        // Assert
        assertThat(history).isNotNull();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getIdempotencyKey()).isEqualTo("test-key-001");
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void shouldReturnEmptyListWhenNoTransactions() {
        // Arrange
        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(sourceWallet));
        when(transactionRepository.findAllByWalletId(anyLong()))
                .thenReturn(List.of());

        // Act
        List<TransactionResponse> history = transactionService
                .getTransactionHistory("sender@fintech.com");

        // Assert
        assertThat(history).isNotNull();
        assertThat(history).isEmpty();
    }
}