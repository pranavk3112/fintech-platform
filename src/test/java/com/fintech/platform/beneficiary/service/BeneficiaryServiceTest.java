package com.fintech.platform.beneficiary.service;

import com.fintech.platform.beneficiary.dto.AddBeneficiaryRequest;
import com.fintech.platform.beneficiary.dto.BeneficiaryResponse;
import com.fintech.platform.beneficiary.dto.BeneficiaryTransferRequest;
import com.fintech.platform.beneficiary.entity.Beneficiary;
import com.fintech.platform.beneficiary.repository.BeneficiaryRepository;
import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.transaction.service.TransactionService;
import com.fintech.platform.upi.service.UpiService;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BeneficiaryService Tests")
class BeneficiaryServiceTest {

    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionService transactionService;
    @Mock private UpiService upiService;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    private User mockUser;
    private Wallet mockWallet;
    private Beneficiary mockUpiBeneficiary;
    private Beneficiary mockWalletBeneficiary;
    private AddBeneficiaryRequest addUpiRequest;
    private AddBeneficiaryRequest addWalletRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).fullName("Test User").email("test@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0)
                .build();

        mockWallet = Wallet.builder()
                .id(2L).user(mockUser)
                .balance(new BigDecimal("5000.00"))
                .status(Wallet.WalletStatus.ACTIVE)
                .walletType(Wallet.WalletType.SAVINGS)
                .upiId("receiver@fintech")
                .upiPinSet(false)
                .dailyLimit(new BigDecimal("100000.00"))
                .monthlyLimit(new BigDecimal("1000000.00"))
                .currency("INR")
                .build();

        mockUpiBeneficiary = Beneficiary.builder()
                .id(1L).user(mockUser)
                .nickname("Mom")
                .beneficiaryType(Beneficiary.BeneficiaryType.UPI)
                .upiId("receiver@fintech")
                .beneficiaryName("Receiver User")
                .isActive(true)
                .build();

        mockWalletBeneficiary = Beneficiary.builder()
                .id(2L).user(mockUser)
                .nickname("Office")
                .beneficiaryType(Beneficiary.BeneficiaryType.WALLET)
                .wallet(mockWallet)
                .beneficiaryName("Office Account")
                .isActive(true)
                .build();

        addUpiRequest = new AddBeneficiaryRequest();
        addUpiRequest.setNickname("Mom");
        addUpiRequest.setBeneficiaryType("UPI");
        addUpiRequest.setUpiId("receiver@fintech");
        addUpiRequest.setBeneficiaryName("Receiver User");

        addWalletRequest = new AddBeneficiaryRequest();
        addWalletRequest.setNickname("Office");
        addWalletRequest.setBeneficiaryType("WALLET");
        addWalletRequest.setWalletId(2L);
        addWalletRequest.setBeneficiaryName("Office Account");
    }

    // ── Add Beneficiary Tests ────────────────────────────────────

    @Test
    @DisplayName("Should add UPI beneficiary successfully")
    void shouldAddUpiBeneficiarySuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.existsByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(false);
        when(walletRepository.findByUpiId(anyString())).thenReturn(Optional.of(mockWallet));
        when(beneficiaryRepository.save(any(Beneficiary.class)))
                .thenReturn(mockUpiBeneficiary);

        BeneficiaryResponse response = beneficiaryService
                .addBeneficiary("test@fintech.com", addUpiRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("Mom");
        assertThat(response.getBeneficiaryType()).isEqualTo("UPI");
        verify(beneficiaryRepository).save(any(Beneficiary.class));
    }

    @Test
    @DisplayName("Should add Wallet beneficiary successfully")
    void shouldAddWalletBeneficiarySuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.existsByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(false);
        when(walletRepository.findById(anyLong())).thenReturn(Optional.of(mockWallet));
        when(beneficiaryRepository.save(any(Beneficiary.class)))
                .thenReturn(mockWalletBeneficiary);

        BeneficiaryResponse response = beneficiaryService
                .addBeneficiary("test@fintech.com", addWalletRequest);

        assertThat(response).isNotNull();
        assertThat(response.getNickname()).isEqualTo("Office");
        assertThat(response.getBeneficiaryType()).isEqualTo("WALLET");
    }

    @Test
    @DisplayName("Should throw exception on duplicate nickname")
    void shouldThrowExceptionOnDuplicateNickname() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.existsByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                beneficiaryService.addBeneficiary("test@fintech.com", addUpiRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(beneficiaryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid beneficiary type")
    void shouldThrowExceptionForInvalidBeneficiaryType() {
        addUpiRequest.setBeneficiaryType("INVALID");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.existsByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                beneficiaryService.addBeneficiary("test@fintech.com", addUpiRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid beneficiary type. Use UPI or WALLET");
    }

    @Test
    @DisplayName("Should throw exception when UPI ID missing for UPI beneficiary")
    void shouldThrowExceptionWhenUpiIdMissingForUpiBeneficiary() {
        addUpiRequest.setUpiId(null);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.existsByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                beneficiaryService.addBeneficiary("test@fintech.com", addUpiRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("UPI ID is required for UPI beneficiary");
    }

    // ── List Beneficiaries Tests ─────────────────────────────────

    @Test
    @DisplayName("Should list all active beneficiaries")
    void shouldListAllActiveBeneficiaries() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndIsActiveTrueOrderByNicknameAsc(anyLong()))
                .thenReturn(List.of(mockUpiBeneficiary, mockWalletBeneficiary));

        List<BeneficiaryResponse> response = beneficiaryService
                .listBeneficiaries("test@fintech.com");

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getNickname()).isEqualTo("Mom");
        assertThat(response.get(1).getNickname()).isEqualTo("Office");
    }

    @Test
    @DisplayName("Should return empty list when no beneficiaries")
    void shouldReturnEmptyListWhenNoBeneficiaries() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndIsActiveTrueOrderByNicknameAsc(anyLong()))
                .thenReturn(List.of());

        List<BeneficiaryResponse> response = beneficiaryService
                .listBeneficiaries("test@fintech.com");

        assertThat(response).isEmpty();
    }

    // ── Delete Beneficiary Tests ─────────────────────────────────

    @Test
    @DisplayName("Should soft delete beneficiary successfully")
    void shouldSoftDeleteBeneficiarySuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.of(mockUpiBeneficiary));
        when(beneficiaryRepository.save(any(Beneficiary.class)))
                .thenReturn(mockUpiBeneficiary);

        beneficiaryService.deleteBeneficiary("test@fintech.com", 1L);

        assertThat(mockUpiBeneficiary.getIsActive()).isFalse();
        verify(beneficiaryRepository).save(mockUpiBeneficiary);
    }

    @Test
    @DisplayName("Should throw exception when beneficiary not found on delete")
    void shouldThrowExceptionWhenBeneficiaryNotFoundOnDelete() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                beneficiaryService.deleteBeneficiary("test@fintech.com", 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Beneficiary not found");
    }

    // ── Transfer to Beneficiary Tests ────────────────────────────

    @Test
    @DisplayName("Should transfer to UPI beneficiary successfully")
    void shouldTransferToUpiBeneficiarySuccessfully() {
        BeneficiaryTransferRequest request = new BeneficiaryTransferRequest();
        request.setNickname("Mom");
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("ben-test-001");
        request.setUpiPin("1234");
        request.setRemarks("Test");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(Optional.of(mockUpiBeneficiary));
        when(upiService.upiTransfer(anyString(), any())).thenReturn(
                com.fintech.platform.upi.dto.UpiTransactionResponse.builder()
                        .status("COMPLETED").build());

        Object response = beneficiaryService
                .transferToBeneficiary("test@fintech.com", request);

        assertThat(response).isNotNull();
        verify(upiService).upiTransfer(anyString(), any());
        verify(transactionService, never()).transfer(anyString(), any());
    }

    @Test
    @DisplayName("Should throw exception when beneficiary not found on transfer")
    void shouldThrowExceptionWhenBeneficiaryNotFoundOnTransfer() {
        BeneficiaryTransferRequest request = new BeneficiaryTransferRequest();
        request.setNickname("Unknown");
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("ben-test-002");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                beneficiaryService.transferToBeneficiary("test@fintech.com", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw exception when UPI PIN missing for UPI beneficiary transfer")
    void shouldThrowExceptionWhenUpiPinMissingForUpiBeneficiaryTransfer() {
        BeneficiaryTransferRequest request = new BeneficiaryTransferRequest();
        request.setNickname("Mom");
        request.setAmount(new BigDecimal("500.00"));
        request.setIdempotencyKey("ben-test-003");
        request.setUpiPin(null);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(beneficiaryRepository.findByUserIdAndNicknameIgnoreCase(anyLong(), anyString()))
                .thenReturn(Optional.of(mockUpiBeneficiary));

        assertThatThrownBy(() ->
                beneficiaryService.transferToBeneficiary("test@fintech.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("UPI PIN is required for UPI transfer");
    }
}