package com.fintech.platform.kyc.repository;

import com.fintech.platform.kyc.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT k FROM KycDocument k WHERE k.user.id = :userId AND k.documentType = :type")
    Optional<KycDocument> findByUserIdAndDocumentType(
            @Param("userId") Long userId,
            @Param("type") KycDocument.DocumentType type);

    List<KycDocument> findByVerificationStatusOrderByCreatedAtAsc(
            KycDocument.VerificationStatus status);
}