package com.fintech.platform.transaction.repository;

import com.fintech.platform.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceWallet.id = :walletId
            OR t.destinationWallet.id = :walletId
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findAllByWalletId(@Param("walletId") Long walletId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.sourceWallet.id = :walletId
            OR t.destinationWallet.id = :walletId)
            AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    List<Transaction> findByWalletIdAndStatus(
            @Param("walletId") Long walletId,
            @Param("status") Transaction.TransactionStatus status);
}