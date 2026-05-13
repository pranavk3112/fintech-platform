package com.fintech.platform.wallet.repository;

import com.fintech.platform.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    @Query("SELECT w FROM Wallet w WHERE w.user.email = :email")
    Optional<Wallet> findByUserEmail(@Param("email") String email);

    @Query("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.id = :id")
    Optional<Wallet> findByIdWithUser(@Param("id") Long id);
}