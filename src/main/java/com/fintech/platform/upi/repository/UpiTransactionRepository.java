package com.fintech.platform.upi.repository;

import com.fintech.platform.upi.entity.UpiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UpiTransactionRepository extends JpaRepository<UpiTransaction, Long> {

    List<UpiTransaction> findBySenderUpiIdOrderByCreatedAtDesc(String senderUpiId);
    List<UpiTransaction> findByReceiverUpiIdOrderByCreatedAtDesc(String receiverUpiId);
}