package com.fintech.platform.beneficiary.repository;

import com.fintech.platform.beneficiary.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByUserIdAndIsActiveTrueOrderByNicknameAsc(Long userId);

    Optional<Beneficiary> findByUserIdAndNicknameIgnoreCase(Long userId, String nickname);

    @Query("SELECT b FROM Beneficiary b WHERE b.user.id = :userId AND b.id = :id AND b.isActive = true")
    Optional<Beneficiary> findByUserIdAndId(@Param("userId") Long userId, @Param("id") Long id);

    boolean existsByUserIdAndNicknameIgnoreCase(Long userId, String nickname);
}