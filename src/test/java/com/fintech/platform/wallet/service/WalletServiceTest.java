package com.fintech.platform.wallet.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
import com.fintech.platform.wallet.dto.FundWalletRequest;
import com.fintech.platform.wallet.dto.WalletResponse;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Tests")
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private User mockUser;
    private Wallet mockWallet;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@fintech.com")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();

        mockWallet = Wallet.builder()
                .id(1L)
                .user(mockUser)
                .balance(new BigDecimal("5000.0000"))
                .status(Wallet.WalletStatus.ACTIVE)
                .build();
    }

    // ── Create Wallet Tests ──────────────────────────────────────

    @Test
    @DisplayName("Should create wallet successfully")
    void shouldCreateWalletSuccessfully() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        // Act
        WalletResponse response = walletService.createWallet("test@fintech.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getOwnerEmail()).isEqualTo("test@fintech.com");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw exception when wallet already exists")
    void shouldThrowExceptionWhenWalletAlreadyExists() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserId(anyLong())).thenReturn(Optional.of(mockWallet));

        // Act & Assert
        assertThatThrownBy(() -> walletService.createWallet("test@fintech.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Wallet already exists for this user");

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletService.createWallet("unknown@fintech.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    // ── Fund Wallet Tests ────────────────────────────────────────

    @Test
    @DisplayName("Should fund wallet successfully")
    void shouldFundWalletSuccessfully() {
        // Arrange
        FundWalletRequest request = new FundWalletRequest();
        request.setAmount(new BigDecimal("1000.00"));

        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.of(mockWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        // Act
        WalletResponse response = walletService.fundWallet("test@fintech.com", request);

        // Assert
        assertThat(response).isNotNull();
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw exception when funding suspended wallet")
    void shouldThrowExceptionWhenFundingSuspendedWallet() {
        // Arrange
        mockWallet.setStatus(Wallet.WalletStatus.SUSPENDED);
        FundWalletRequest request = new FundWalletRequest();
        request.setAmount(new BigDecimal("1000.00"));

        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.of(mockWallet));

        // Act & Assert
        assertThatThrownBy(() -> walletService.fundWallet("test@fintech.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Wallet is not active");

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    // ── Debit Wallet Tests ───────────────────────────────────────

    @Test
    @DisplayName("Should debit wallet successfully")
    void shouldDebitWalletSuccessfully() {
        // Arrange
        when(walletRepository.save(any(Wallet.class))).thenReturn(mockWallet);

        // Act
        walletService.debitWallet(mockWallet, new BigDecimal("1000.00"));

        // Assert
        assertThat(mockWallet.getBalance())
                .isEqualByComparingTo(new BigDecimal("4000.0000"));
        verify(walletRepository).save(mockWallet);
    }

    @Test
    @DisplayName("Should throw exception on insufficient balance")
    void shouldThrowExceptionOnInsufficientBalance() {
        // Act & Assert
        assertThatThrownBy(() ->
                walletService.debitWallet(mockWallet, new BigDecimal("99999.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient balance");

        verify(walletRepository, never()).save(any(Wallet.class));
    }
}