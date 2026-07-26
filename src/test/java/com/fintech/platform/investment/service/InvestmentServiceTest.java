package com.fintech.platform.investment.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.investment.dto.*;
import com.fintech.platform.investment.entity.FixedDeposit;
import com.fintech.platform.investment.entity.RdInstallment;
import com.fintech.platform.investment.entity.RecurringDeposit;
import com.fintech.platform.investment.repository.FixedDepositRepository;
import com.fintech.platform.investment.repository.RdInstallmentRepository;
import com.fintech.platform.investment.repository.RecurringDepositRepository;
import com.fintech.platform.investment.util.InterestCalculator;
import com.fintech.platform.transaction.repository.TransactionRepository;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentService Tests")
class InvestmentServiceTest {

    @Mock private FixedDepositRepository fdRepository;
    @Mock private RecurringDepositRepository rdRepository;
    @Mock private RdInstallmentRepository rdInstallmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletService walletService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private InterestCalculator interestCalculator;

    @InjectMocks
    private InvestmentService investmentService;

    private User mockUser;
    private Wallet mockWallet;
    private FixedDeposit mockFd;
    private RecurringDeposit mockRd;
    private CreateFdRequest fdRequest;
    private CreateRdRequest rdRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).fullName("Test User").email("test@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0)
                .build();

        mockWallet = Wallet.builder()
                .id(1L).user(mockUser)
                .balance(new BigDecimal("50000.00"))
                .status(Wallet.WalletStatus.ACTIVE)
                .walletType(Wallet.WalletType.SAVINGS)
                .upiId("test@fintech")
                .upiPinSet(false)
                .dailyLimit(new BigDecimal("100000.00"))
                .monthlyLimit(new BigDecimal("1000000.00"))
                .currency("INR")
                .build();

        mockFd = FixedDeposit.builder()
                .id(1L).user(mockUser).wallet(mockWallet)
                .fdNumber("FD202607260001")
                .principalAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("7.00"))
                .tenureMonths(12)
                .interestType(FixedDeposit.InterestType.COMPOUND)
                .compoundingFrequency(FixedDeposit.CompoundingFrequency.QUARTERLY)
                .maturityAmount(new BigDecimal("10724.50"))
                .interestEarned(new BigDecimal("724.50"))
                .status(FixedDeposit.FdStatus.ACTIVE)
                .startDate(LocalDate.now())
                .maturityDate(LocalDate.now().plusMonths(12))
                .prematureWithdrawalPenalty(new BigDecimal("1.00"))
                .build();

        mockRd = RecurringDeposit.builder()
                .id(1L).user(mockUser).wallet(mockWallet)
                .rdNumber("RD202607260001")
                .monthlyInstallment(new BigDecimal("1000.00"))
                .interestRate(new BigDecimal("6.50"))
                .tenureMonths(6)
                .totalDeposited(new BigDecimal("1000.00"))
                .maturityAmount(new BigDecimal("6197.00"))
                .interestEarned(new BigDecimal("197.00"))
                .status(RecurringDeposit.RdStatus.ACTIVE)
                .startDate(LocalDate.now())
                .maturityDate(LocalDate.now().plusMonths(6))
                .nextInstallmentDate(LocalDate.now().plusMonths(1))
                .installmentsPaid(1)
                .missedInstallments(0)
                .build();

        fdRequest = new CreateFdRequest();
        fdRequest.setPrincipalAmount(new BigDecimal("10000.00"));
        fdRequest.setTenureMonths(12);
        fdRequest.setCompoundingFrequency("QUARTERLY");

        rdRequest = new CreateRdRequest();
        rdRequest.setMonthlyInstallment(new BigDecimal("1000.00"));
        rdRequest.setTenureMonths(6);
    }

    // ── FD Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should create FD successfully")
    void shouldCreateFdSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.of(mockWallet));
        when(interestCalculator.getCompoundingFrequency(anyString())).thenReturn(4);
        when(interestCalculator.calculateFdMaturityAmount(any(), any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("10724.5000"));
        when(fdRepository.save(any(FixedDeposit.class))).thenReturn(mockFd);
        when(transactionRepository.save(any())).thenReturn(null);

        FdResponse response = investmentService.createFd("test@fintech.com", fdRequest);

        assertThat(response).isNotNull();
        assertThat(response.getFdNumber()).isEqualTo("FD202607260001");
        assertThat(response.getPrincipalAmount())
                .isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(walletService).debitWallet(eq(mockWallet), eq(new BigDecimal("10000.00")));
        verify(fdRepository).save(any(FixedDeposit.class));
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance for FD")
    void shouldThrowExceptionWhenInsufficientBalanceForFd() {
        mockWallet.setBalance(new BigDecimal("500.00"));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.of(mockWallet));

        assertThatThrownBy(() ->
                investmentService.createFd("test@fintech.com", fdRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient wallet balance for FD creation");

        verify(fdRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found for FD")
    void shouldThrowExceptionWhenWalletNotFoundForFd() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                investmentService.createFd("test@fintech.com", fdRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Wallet not found");
    }

    @Test
    @DisplayName("Should close FD prematurely with penalty")
    void shouldCloseFdPrematurelyWithPenalty() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(fdRepository.findById(anyLong())).thenReturn(Optional.of(mockFd));
        when(interestCalculator.calculateFdMaturityAmount(any(), any(), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("10300.00"));
        when(fdRepository.save(any(FixedDeposit.class))).thenReturn(mockFd);
        when(transactionRepository.save(any())).thenReturn(null);

        FdResponse response = investmentService.closeFd("test@fintech.com", 1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PREMATURE_CLOSED");
        verify(walletService).creditWallet(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when closing FD belonging to different user")
    void shouldThrowExceptionWhenClosingFdOfDifferentUser() {
        User otherUser = User.builder()
                .id(2L).fullName("Other").email("other@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0).build();

        mockFd.setUser(otherUser);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(fdRepository.findById(anyLong())).thenReturn(Optional.of(mockFd));

        assertThatThrownBy(() ->
                investmentService.closeFd("test@fintech.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("FD does not belong to this user");
    }

    @Test
    @DisplayName("Should throw exception when closing already closed FD")
    void shouldThrowExceptionWhenClosingAlreadyClosedFd() {
        mockFd.setStatus(FixedDeposit.FdStatus.CLOSED);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(fdRepository.findById(anyLong())).thenReturn(Optional.of(mockFd));

        assertThatThrownBy(() ->
                investmentService.closeFd("test@fintech.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("FD is not active");
    }

    @Test
    @DisplayName("Should get all FDs for user")
    void shouldGetAllFdsForUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(fdRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(mockFd));

        List<FdResponse> response = investmentService.getUserFds("test@fintech.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFdNumber()).isEqualTo("FD202607260001");
    }

    // ── RD Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should create RD successfully")
    void shouldCreateRdSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.of(mockWallet));
        when(interestCalculator.calculateRdMaturityAmount(any(), any(), anyInt()))
                .thenReturn(new BigDecimal("6197.0000"));
        when(rdRepository.save(any(RecurringDeposit.class))).thenReturn(mockRd);
        when(rdInstallmentRepository.save(any(RdInstallment.class))).thenReturn(null);
        when(transactionRepository.save(any())).thenReturn(null);

        RdResponse response = investmentService.createRd("test@fintech.com", rdRequest);

        assertThat(response).isNotNull();
        assertThat(response.getRdNumber()).isEqualTo("RD202607260001");
        assertThat(response.getMonthlyInstallment())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(walletService).debitWallet(eq(mockWallet), eq(new BigDecimal("1000.00")));
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance for RD")
    void shouldThrowExceptionWhenInsufficientBalanceForRd() {
        mockWallet.setBalance(new BigDecimal("100.00"));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUserEmail(anyString())).thenReturn(Optional.of(mockWallet));

        assertThatThrownBy(() ->
                investmentService.createRd("test@fintech.com", rdRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient balance for first RD installment");

        verify(rdRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should pay RD installment successfully")
    void shouldPayRdInstallmentSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(rdRepository.findById(anyLong())).thenReturn(Optional.of(mockRd));
        when(rdRepository.save(any(RecurringDeposit.class))).thenReturn(mockRd);
        when(rdInstallmentRepository.save(any(RdInstallment.class))).thenReturn(null);
        when(transactionRepository.save(any())).thenReturn(null);

        RdResponse response = investmentService.payRdInstallment("test@fintech.com", 1L);

        assertThat(response).isNotNull();
        verify(walletService).debitWallet(eq(mockWallet), eq(new BigDecimal("1000.00")));
        verify(rdInstallmentRepository).save(any(RdInstallment.class));
    }

    @Test
    @DisplayName("Should throw exception when paying RD of different user")
    void shouldThrowExceptionWhenPayingRdOfDifferentUser() {
        User otherUser = User.builder()
                .id(2L).fullName("Other").email("other@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0).build();

        mockRd.setUser(otherUser);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(rdRepository.findById(anyLong())).thenReturn(Optional.of(mockRd));

        assertThatThrownBy(() ->
                investmentService.payRdInstallment("test@fintech.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("RD does not belong to this user");
    }

    @Test
    @DisplayName("Should throw exception when all RD installments already paid")
    void shouldThrowExceptionWhenAllInstallmentsPaid() {
        mockRd.setInstallmentsPaid(6);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(rdRepository.findById(anyLong())).thenReturn(Optional.of(mockRd));

        assertThatThrownBy(() ->
                investmentService.payRdInstallment("test@fintech.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("All installments already paid");
    }

    @Test
    @DisplayName("Should get all RDs for user")
    void shouldGetAllRdsForUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(rdRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(mockRd));

        List<RdResponse> response = investmentService.getUserRds("test@fintech.com");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRdNumber()).isEqualTo("RD202607260001");
    }

    @Test
    @DisplayName("Should get RD installments successfully")
    void shouldGetRdInstallmentsSuccessfully() {
        RdInstallment installment = RdInstallment.builder()
                .id(1L).recurringDeposit(mockRd)
                .installmentNumber(1)
                .amount(new BigDecimal("1000.00"))
                .status(RdInstallment.InstallmentStatus.PAID)
                .dueDate(LocalDate.now())
                .paidDate(LocalDateTime.now())
                .penaltyAmount(BigDecimal.ZERO)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(rdRepository.findById(anyLong())).thenReturn(Optional.of(mockRd));
        when(rdInstallmentRepository
                .findByRecurringDepositIdOrderByInstallmentNumberAsc(anyLong()))
                .thenReturn(List.of(installment));

        List<RdInstallmentResponse> response = investmentService
                .getRdInstallments("test@fintech.com", 1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(response.get(0).getStatus()).isEqualTo("PAID");
    }
}