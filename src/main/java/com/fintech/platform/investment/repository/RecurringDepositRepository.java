package com.fintech.platform.investment.repository;

import com.fintech.platform.investment.entity.RecurringDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {

    List<RecurringDeposit> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecurringDeposit> findByStatusAndNextInstallmentDateLessThanEqual(
            RecurringDeposit.RdStatus status, LocalDate date);

    Optional<RecurringDeposit> findByRdNumber(String rdNumber);

    List<RecurringDeposit> findByUserIdAndStatus(Long userId, RecurringDeposit.RdStatus status);
}