package com.fintech.platform.investment.repository;

import com.fintech.platform.investment.entity.RdInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RdInstallmentRepository extends JpaRepository<RdInstallment, Long> {

    List<RdInstallment> findByRecurringDepositIdOrderByInstallmentNumberAsc(Long rdId);

    long countByRecurringDepositIdAndStatus(Long rdId, RdInstallment.InstallmentStatus status);
}