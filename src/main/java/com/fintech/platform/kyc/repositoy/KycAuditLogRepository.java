package com.fintech.platform.kyc.repository;

import com.fintech.platform.kyc.entity.KycAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KycAuditLogRepository extends JpaRepository<KycAuditLog, Long> {
    List<KycAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}