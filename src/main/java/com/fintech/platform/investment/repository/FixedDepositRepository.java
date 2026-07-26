package com.fintech.platform.investment.repository;

import com.fintech.platform.investment.entity.FixedDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {

    List<FixedDeposit> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<FixedDeposit> findByStatusAndMaturityDateLessThanEqual(
            FixedDeposit.FdStatus status, LocalDate date);

    Optional<FixedDeposit> findByFdNumber(String fdNumber);

    List<FixedDeposit> findByUserIdAndStatus(Long userId, FixedDeposit.FdStatus status);
}