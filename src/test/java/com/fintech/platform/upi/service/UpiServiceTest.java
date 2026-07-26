package com.fintech.platform.upi.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.transaction.dto.TransactionResponse;
import com.fintech.platform.transaction.service.TransactionService;
import com.fintech.platform.upi.dto.SetUpiPinRequest;
import com.fintech.platform.upi.dto.UpiTransferRequest;
import com.fintech.platform.upi.dto.UpiTransactionResponse;
import com.fintech.platform.upi.dto.VpaValidationResponse;
import com.fintech.platform.upi.entity.UpiTransaction;
import com.fintech.platform.upi.repository.UpiTransactionRepository;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpiService Tests")
class UpiServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UpiTransactionRepository upiTransactionRepository;
    @Mock private TransactionService transactionService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UpiTransactionRecorder upiTransactionRecorder;

    @InjectMocks
    private UpiService upiService;

    private User senderUser;
    private User receiverUser;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private UpiTransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L).fullName("Sender").email("sender@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true).build();

        receiverUser = User.builder()
                .id(2L).fullName("Receiver").email("receiver@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true).build();

        senderWallet = Wallet.builder()
                .id(1L).user(senderUser)
                .balance(new BigDecimal("5000.00"))
                .status(Wallet.WalletStatus.ACTIVE)
                .walletType(Wallet.WalletType.SAVINGS)
                .upiId("sender@fintech")
                .upiPinSet(true)
                .upiPinHash("hashedPin")
                .dailyLimit(new BigDecimal("100000.00"))
                .monthlyLimit(new BigDecimal("1000000.00"))
                .currency("INR")
                .build();

        receiverWallet = Wallet.builder()
                .id(2L).user(receiverUser)
                .balance(new BigDecimal("1000.00"))
                .status(Wallet.WalletStatus.ACTIVE)
                .walletType(Wallet.WalletType.SAVINGS)
                .upiId("receiver@fintech")
                .upiPinSet(false)
                .dailyLimit(new BigDecimal("100000.00"))
                .monthlyLimit(new BigDecimal("1000000.00"))
                .currency("INR")
                .build();

        transferRequest = new UpiTransferRequest();
        transferRequest.setReceiverUpiId("receiver@fintech");
        transferRequest.setAmount(new BigDecimal("500.00"));
        transferRequest.setUpiPin("1234");
        transferRequest.setIdempotencyKey("upi-test-001");
        transferRequest.setRemarks("Test transfer");
    }

    // ── Set UPI PIN Tests ────────────────────────────────────────

    @Test
    @DisplayName("Should set UPI PIN successfully")
    void shouldSetUpiPinSuccessfully() {
        SetUpiPinRequest request = new SetUpiPinRequest();
        request.setUpiPin("1234");

        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPin");
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);

        upiService.setUpiPin("sender@fintech.com", request);

        verify(walletRepository).save(any(Wallet.class));
        verify(passwordEncoder).encode("1234");
    }

    @Test
    @DisplayName("Should throw exception when wallet not found on PIN set")
    void shouldThrowExceptionWhenWalletNotFoundOnPinSet() {
        SetUpiPinRequest request = new SetUpiPinRequest();
        request.setUpiPin("1234");

        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> upiService.setUpiPin("unknown@fintech.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Wallet not found");
    }

    // ── Validate VPA Tests ───────────────────────────────────────

    @Test
    @DisplayName("Should return valid VPA response when UPI ID exists")
    void shouldReturnValidVpaWhenUpiIdExists() {
        when(walletRepository.findByUpiId(anyString()))
                .thenReturn(Optional.of(receiverWallet));

        VpaValidationResponse response = upiService.validateVpa("receiver@fintech");

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUpiId()).isEqualTo("receiver@fintech");
        assertThat(response.getAccountHolderName()).isEqualTo("Receiver");
    }

    @Test
    @DisplayName("Should return invalid VPA response when UPI ID not found")
    void shouldReturnInvalidVpaWhenUpiIdNotFound() {
        when(walletRepository.findByUpiId(anyString()))
                .thenReturn(Optional.empty());

        VpaValidationResponse response = upiService.validateVpa("unknown@fintech");

        assertThat(response.isValid()).isFalse();
        assertThat(response.getAccountHolderName()).isNull();
    }

    // ── UPI Transfer Tests ───────────────────────────────────────

    @Test
    @DisplayName("Should complete UPI transfer successfully")
    void shouldCompleteUpiTransferSuccessfully() {
        UpiTransaction savedUpiTx = UpiTransaction.builder()
                .id(1L)
                .senderUpiId("sender@fintech")
                .receiverUpiId("receiver@fintech")
                .amount(new BigDecimal("500.00"))
                .status("COMPLETED")
                .idempotencyKey("upi-test-001")
                .build();

        TransactionResponse txResponse = TransactionResponse.builder()
                .id(1L).status("COMPLETED")
                .amount(new BigDecimal("500.00"))
                .build();

        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(walletRepository.findByUpiId(anyString()))
                .thenReturn(Optional.of(receiverWallet));
        when(upiTransactionRepository.findCompletedByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());
        when(transactionService.transfer(anyString(), any()))
                .thenReturn(txResponse);
        when(upiTransactionRecorder.save(any())).thenReturn(savedUpiTx);

        UpiTransactionResponse response = upiService
                .upiTransfer("sender@fintech.com", transferRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(transactionService).transfer(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when UPI PIN not set")
    void shouldThrowExceptionWhenUpiPinNotSet() {
        senderWallet.setUpiPinSet(false);

        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(senderWallet));

        assertThatThrownBy(() ->
                upiService.upiTransfer("sender@fintech.com", transferRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("UPI PIN not set. Please set your UPI PIN first.");
    }

    @Test
    @DisplayName("Should throw exception when UPI PIN is invalid")
    void shouldThrowExceptionWhenUpiPinIsInvalid() {
        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                upiService.upiTransfer("sender@fintech.com", transferRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid UPI PIN");
    }

    @Test
    @DisplayName("Should return existing transaction on duplicate completed key")
    void shouldReturnExistingTransactionOnDuplicateCompletedKey() {
        UpiTransaction existingTx = UpiTransaction.builder()
                .id(1L)
                .senderUpiId("sender@fintech")
                .receiverUpiId("receiver@fintech")
                .amount(new BigDecimal("500.00"))
                .status("COMPLETED")
                .idempotencyKey("upi-test-001")
                .build();

        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(walletRepository.findByUpiId(anyString()))
                .thenReturn(Optional.of(receiverWallet));
        when(upiTransactionRepository.findCompletedByIdempotencyKey(anyString()))
                .thenReturn(Optional.of(existingTx));

        UpiTransactionResponse response = upiService
                .upiTransfer("sender@fintech.com", transferRequest);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(transactionService, never()).transfer(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when transferring to own UPI ID")
    void shouldThrowExceptionWhenTransferringToOwnUpiId() {
        transferRequest.setReceiverUpiId("sender@fintech");

        when(walletRepository.findByUserEmail(anyString()))
                .thenReturn(Optional.of(senderWallet));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(walletRepository.findByUpiId(anyString()))
                .thenReturn(Optional.of(senderWallet));

        assertThatThrownBy(() ->
                upiService.upiTransfer("sender@fintech.com", transferRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot transfer to your own UPI ID");
    }
}