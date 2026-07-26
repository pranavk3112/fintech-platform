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
import com.fintech.platform.transaction.entity.Transaction;
import com.fintech.platform.transaction.repository.TransactionRepository;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
import com.fintech.platform.wallet.entity.Wallet;
import com.fintech.platform.wallet.repository.WalletRepository;
import com.fintech.platform.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final FixedDepositRepository fdRepository;
    private final RecurringDepositRepository rdRepository;
    private final RdInstallmentRepository rdInstallmentRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final InterestCalculator interestCalculator;

    @Value("${investment.fd.interest-rate.default:7.00}")
    private BigDecimal fdDefaultRate;

    @Value("${investment.fd.penalty.premature-withdrawal:1.00}")
    private BigDecimal prematureWithdrawalPenalty;

    @Value("${investment.rd.interest-rate.default:6.50}")
    private BigDecimal rdDefaultRate;

    private static final AtomicLong fdCounter = new AtomicLong(1);
    private static final AtomicLong rdCounter = new AtomicLong(1);

    // ── Create FD ────────────────────────────────────────────────
    @Transactional
    public FdResponse createFd(String email, CreateFdRequest request) {
        log.info("Creating FD for user: {} amount: {}", email, request.getPrincipalAmount());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // Check sufficient balance
        if (wallet.getBalance().compareTo(request.getPrincipalAmount()) < 0) {
            throw new BadRequestException("Insufficient wallet balance for FD creation");
        }

        // Calculate interest and maturity
        int compoundingFreq = interestCalculator.getCompoundingFrequency(
                request.getCompoundingFrequency());

        BigDecimal maturityAmount = interestCalculator.calculateFdMaturityAmount(
                request.getPrincipalAmount(),
                fdDefaultRate,
                request.getTenureMonths(),
                compoundingFreq);

        BigDecimal interestEarned = maturityAmount.subtract(request.getPrincipalAmount());

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());

        // Generate FD number
        String fdNumber = generateFdNumber();

        // Debit wallet
        walletService.debitWallet(wallet, request.getPrincipalAmount());

        // Record transaction
        recordInvestmentTransaction(wallet, request.getPrincipalAmount(),
                "FD Created: " + fdNumber, Transaction.TransactionType.DEBIT);

        // Create FD
        FixedDeposit fd = FixedDeposit.builder()
                .user(user)
                .wallet(wallet)
                .fdNumber(fdNumber)
                .principalAmount(request.getPrincipalAmount())
                .interestRate(fdDefaultRate)
                .tenureMonths(request.getTenureMonths())
                .interestType(FixedDeposit.InterestType.COMPOUND)
                .compoundingFrequency(FixedDeposit.CompoundingFrequency.valueOf(
                        request.getCompoundingFrequency().toUpperCase()))
                .maturityAmount(maturityAmount)
                .interestEarned(interestEarned)
                .status(FixedDeposit.FdStatus.ACTIVE)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .prematureWithdrawalPenalty(prematureWithdrawalPenalty)
                .build();

        FixedDeposit saved = fdRepository.save(fd);
        log.info("FD created: {} for user: {}", fdNumber, email);

        return mapFdToResponse(saved);
    }

    // ── Close FD (Premature or Maturity) ────────────────────────
    @Transactional
    public FdResponse closeFd(String email, Long fdId) {
        log.info("Closing FD: {} for user: {}", fdId, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FixedDeposit fd = fdRepository.findById(fdId)
                .orElseThrow(() -> new ResourceNotFoundException("FD not found"));

        // Ownership check
        if (!fd.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("FD does not belong to this user");
        }

        if (fd.getStatus() != FixedDeposit.FdStatus.ACTIVE) {
            throw new BadRequestException("FD is not active");
        }

        Wallet wallet = fd.getWallet();
        BigDecimal amountToCredit;
        boolean isPremature = LocalDate.now().isBefore(fd.getMaturityDate());

        if (isPremature) {
            // Apply penalty — reduce interest rate by penalty percentage
            BigDecimal penalizedRate = fd.getInterestRate()
                    .subtract(fd.getPrematureWithdrawalPenalty());
            int monthsCompleted = (int) (LocalDate.now().toEpochDay()
                    - fd.getStartDate().toEpochDay()) / 30;

            BigDecimal penalizedMaturity = interestCalculator.calculateFdMaturityAmount(
                    fd.getPrincipalAmount(),
                    penalizedRate.max(BigDecimal.ZERO),
                    monthsCompleted,
                    4);

            amountToCredit = penalizedMaturity;
            fd.setStatus(FixedDeposit.FdStatus.PREMATURE_CLOSED);
            fd.setInterestEarned(penalizedMaturity.subtract(fd.getPrincipalAmount()));
            fd.setMaturityAmount(penalizedMaturity);

            log.info("Premature FD closure. Penalty applied. Amount: {}", amountToCredit);
        } else {
            amountToCredit = fd.getMaturityAmount();
            fd.setStatus(FixedDeposit.FdStatus.MATURED);
        }

        // Credit wallet
        walletService.creditWallet(wallet, amountToCredit);

        // Record transaction
        recordInvestmentTransaction(wallet, amountToCredit,
                "FD Closed: " + fd.getFdNumber(), Transaction.TransactionType.CREDIT);

        fdRepository.save(fd);
        log.info("FD closed: {} amount credited: {}", fd.getFdNumber(), amountToCredit);

        return mapFdToResponse(fd);
    }

    // ── Get All FDs for user ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<FdResponse> getUserFds(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return fdRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapFdToResponse).collect(Collectors.toList());
    }

    // ── Create RD ────────────────────────────────────────────────
    @Transactional
    public RdResponse createRd(String email, CreateRdRequest request) {
        log.info("Creating RD for user: {} installment: {}", email, request.getMonthlyInstallment());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        // Check sufficient balance for first installment
        if (wallet.getBalance().compareTo(request.getMonthlyInstallment()) < 0) {
            throw new BadRequestException("Insufficient balance for first RD installment");
        }

        // Calculate maturity amount
        BigDecimal maturityAmount = interestCalculator.calculateRdMaturityAmount(
                request.getMonthlyInstallment(),
                rdDefaultRate,
                request.getTenureMonths());

        BigDecimal totalDeposited = request.getMonthlyInstallment()
                .multiply(BigDecimal.valueOf(request.getTenureMonths()));

        BigDecimal interestEarned = maturityAmount.subtract(totalDeposited);

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(request.getTenureMonths());
        LocalDate nextInstallmentDate = startDate.plusMonths(1);

        String rdNumber = generateRdNumber();

        // Debit first installment from wallet
        walletService.debitWallet(wallet, request.getMonthlyInstallment());

        // Record transaction
        recordInvestmentTransaction(wallet, request.getMonthlyInstallment(),
                "RD Installment 1: " + rdNumber, Transaction.TransactionType.DEBIT);

        // Create RD
        RecurringDeposit rd = RecurringDeposit.builder()
                .user(user)
                .wallet(wallet)
                .rdNumber(rdNumber)
                .monthlyInstallment(request.getMonthlyInstallment())
                .interestRate(rdDefaultRate)
                .tenureMonths(request.getTenureMonths())
                .totalDeposited(request.getMonthlyInstallment())
                .maturityAmount(maturityAmount)
                .interestEarned(interestEarned)
                .status(RecurringDeposit.RdStatus.ACTIVE)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .nextInstallmentDate(nextInstallmentDate)
                .installmentsPaid(1)
                .missedInstallments(0)
                .build();

        RecurringDeposit saved = rdRepository.save(rd);

        // Record first installment
        RdInstallment firstInstallment = RdInstallment.builder()
                .recurringDeposit(saved)
                .installmentNumber(1)
                .amount(request.getMonthlyInstallment())
                .status(RdInstallment.InstallmentStatus.PAID)
                .dueDate(startDate)
                .paidDate(LocalDateTime.now())
                .penaltyAmount(BigDecimal.ZERO)
                .build();

        rdInstallmentRepository.save(firstInstallment);

        log.info("RD created: {} for user: {}", rdNumber, email);
        return mapRdToResponse(saved);
    }

    // ── Pay RD Installment ───────────────────────────────────────
    @Transactional
    public RdResponse payRdInstallment(String email, Long rdId) {
        log.info("Paying RD installment: {} for user: {}", rdId, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RecurringDeposit rd = rdRepository.findById(rdId)
                .orElseThrow(() -> new ResourceNotFoundException("RD not found"));

        if (!rd.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("RD does not belong to this user");
        }

        if (rd.getStatus() != RecurringDeposit.RdStatus.ACTIVE) {
            throw new BadRequestException("RD is not active");
        }

        if (rd.getInstallmentsPaid() >= rd.getTenureMonths()) {
            throw new BadRequestException("All installments already paid");
        }

        Wallet wallet = rd.getWallet();

        // Check for late payment
        boolean isLate = LocalDate.now().isAfter(rd.getNextInstallmentDate());
        BigDecimal penaltyAmount = BigDecimal.ZERO;

        if (isLate) {
            // Penalty = ₹50 flat for simplicity
            penaltyAmount = new BigDecimal("50.00");
            log.info("Late RD payment. Penalty: {}", penaltyAmount);
        }

        BigDecimal totalDebit = rd.getMonthlyInstallment().add(penaltyAmount);

        if (wallet.getBalance().compareTo(totalDebit) < 0) {
            throw new BadRequestException("Insufficient balance for RD installment");
        }

        // Debit installment + penalty
        walletService.debitWallet(wallet, totalDebit);

        // Record transaction
        recordInvestmentTransaction(wallet, totalDebit,
                "RD Installment " + (rd.getInstallmentsPaid() + 1) + ": " + rd.getRdNumber(),
                Transaction.TransactionType.DEBIT);

        // Update RD
        rd.setInstallmentsPaid(rd.getInstallmentsPaid() + 1);
        rd.setTotalDeposited(rd.getTotalDeposited().add(rd.getMonthlyInstallment()));
        rd.setNextInstallmentDate(rd.getNextInstallmentDate().plusMonths(1));

        // Check if all installments paid
        if (rd.getInstallmentsPaid() >= rd.getTenureMonths()) {
            rd.setStatus(RecurringDeposit.RdStatus.MATURED);
            // Credit maturity amount
            walletService.creditWallet(wallet, rd.getMaturityAmount());
            recordInvestmentTransaction(wallet, rd.getMaturityAmount(),
                    "RD Matured: " + rd.getRdNumber(), Transaction.TransactionType.CREDIT);
            log.info("RD matured: {} maturity amount credited: {}",
                    rd.getRdNumber(), rd.getMaturityAmount());
        }

        // Record installment
        RdInstallment installment = RdInstallment.builder()
                .recurringDeposit(rd)
                .installmentNumber(rd.getInstallmentsPaid())
                .amount(rd.getMonthlyInstallment())
                .status(isLate ? RdInstallment.InstallmentStatus.LATE_PAID
                        : RdInstallment.InstallmentStatus.PAID)
                .dueDate(rd.getNextInstallmentDate().minusMonths(1))
                .paidDate(LocalDateTime.now())
                .penaltyAmount(penaltyAmount)
                .build();

        rdInstallmentRepository.save(installment);
        rdRepository.save(rd);

        return mapRdToResponse(rd);
    }

    // ── Get All RDs for user ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RdResponse> getUserRds(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return rdRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapRdToResponse).collect(Collectors.toList());
    }

    // ── Get RD Installments ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<RdInstallmentResponse> getRdInstallments(String email, Long rdId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RecurringDeposit rd = rdRepository.findById(rdId)
                .orElseThrow(() -> new ResourceNotFoundException("RD not found"));

        if (!rd.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("RD does not belong to this user");
        }

        return rdInstallmentRepository
                .findByRecurringDepositIdOrderByInstallmentNumberAsc(rdId)
                .stream()
                .map(this::mapInstallmentToResponse)
                .collect(Collectors.toList());
    }

    private RdInstallmentResponse mapInstallmentToResponse(RdInstallment installment) {
        return RdInstallmentResponse.builder()
                .id(installment.getId())
                .installmentNumber(installment.getInstallmentNumber())
                .amount(installment.getAmount())
                .status(installment.getStatus().name())
                .dueDate(installment.getDueDate())
                .paidDate(installment.getPaidDate())
                .penaltyAmount(installment.getPenaltyAmount())
                .createdAt(installment.getCreatedAt())
                .build();
    }

    // ── Private helpers ──────────────────────────────────────────
    private void recordInvestmentTransaction(Wallet wallet, BigDecimal amount,
                                             String description,
                                             Transaction.TransactionType type) {
        Transaction transaction = Transaction.builder()
                .idempotencyKey("INV-" + System.currentTimeMillis())
                .sourceWallet(type == Transaction.TransactionType.DEBIT ? wallet : null)
                .destinationWallet(type == Transaction.TransactionType.CREDIT ? wallet : null)
                .amount(amount)
                .balanceBeforeSource(wallet.getBalance())
                .balanceAfterSource(wallet.getBalance())
                .type(type)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(description)
                .build();

        transactionRepository.save(transaction);
    }

    private String generateFdNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "FD" + date + String.format("%04d", fdCounter.getAndIncrement());
    }

    private String generateRdNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "RD" + date + String.format("%04d", rdCounter.getAndIncrement());
    }

    private FdResponse mapFdToResponse(FixedDeposit fd) {
        return FdResponse.builder()
                .id(fd.getId())
                .fdNumber(fd.getFdNumber())
                .principalAmount(fd.getPrincipalAmount())
                .interestRate(fd.getInterestRate())
                .tenureMonths(fd.getTenureMonths())
                .interestType(fd.getInterestType().name())
                .compoundingFrequency(fd.getCompoundingFrequency().name())
                .maturityAmount(fd.getMaturityAmount())
                .interestEarned(fd.getInterestEarned())
                .status(fd.getStatus().name())
                .startDate(fd.getStartDate())
                .maturityDate(fd.getMaturityDate())
                .prematureWithdrawalPenalty(fd.getPrematureWithdrawalPenalty())
                .createdAt(fd.getCreatedAt())
                .build();
    }

    private RdResponse mapRdToResponse(RecurringDeposit rd) {
        return RdResponse.builder()
                .id(rd.getId())
                .rdNumber(rd.getRdNumber())
                .monthlyInstallment(rd.getMonthlyInstallment())
                .interestRate(rd.getInterestRate())
                .tenureMonths(rd.getTenureMonths())
                .totalDeposited(rd.getTotalDeposited())
                .maturityAmount(rd.getMaturityAmount())
                .interestEarned(rd.getInterestEarned())
                .status(rd.getStatus().name())
                .startDate(rd.getStartDate())
                .maturityDate(rd.getMaturityDate())
                .nextInstallmentDate(rd.getNextInstallmentDate())
                .installmentsPaid(rd.getInstallmentsPaid())
                .missedInstallments(rd.getMissedInstallments())
                .createdAt(rd.getCreatedAt())
                .build();
    }
}