package com.fintech.platform.upi.repository;

import com.fintech.platform.upi.entity.UpiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpiTransactionRepository extends JpaRepository<UpiTransaction, Long> {

    List<UpiTransaction> findBySenderUpiIdOrderByCreatedAtDesc(String senderUpiId);
    List<UpiTransaction> findByReceiverUpiIdOrderByCreatedAtDesc(String receiverUpiId);
    Optional<UpiTransaction> findByIdempotencyKey(String idempotencyKey);
    @Query("SELECT u FROM UpiTransaction u WHERE u.idempotencyKey = :key AND u.status = 'COMPLETED'")
    Optional<UpiTransaction> findCompletedByIdempotencyKey(@Param("key") String key);
}