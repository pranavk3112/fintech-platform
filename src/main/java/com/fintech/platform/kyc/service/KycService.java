package com.fintech.platform.kyc.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.kyc.dto.*;
import com.fintech.platform.kyc.entity.KycAuditLog;
import com.fintech.platform.kyc.entity.KycDocument;
import com.fintech.platform.kyc.repository.KycAuditLogRepository;
import com.fintech.platform.kyc.repository.KycDocumentRepository;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final KycAuditLogRepository kycAuditLogRepository;
    private final UserRepository userRepository;

    // ── Submit KYC Document ──────────────────────────────────────
    @Transactional
    public KycDocumentResponse submitDocument(String email,
                                              KycSubmissionRequest request) {
        log.info("KYC document submission by: {} type: {}",
                email, request.getDocumentType());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Parse document type
        KycDocument.DocumentType documentType;
        try {
            documentType = KycDocument.DocumentType.valueOf(
                    request.getDocumentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid document type: " + request.getDocumentType());
        }

        // Check if same document type already submitted and verified
        kycDocumentRepository.findByUserIdAndDocumentType(user.getId(), documentType)
                .ifPresent(existing -> {
                    if (existing.getVerificationStatus() ==
                            KycDocument.VerificationStatus.VERIFIED) {
                        throw new BadRequestException(
                                documentType + " is already verified");
                    }
                });

        KycDocument document = KycDocument.builder()
                .user(user)
                .documentType(documentType)
                .documentNumber(request.getDocumentNumber())
                .documentUrl(request.getDocumentUrl())
                .verificationStatus(KycDocument.VerificationStatus.PENDING)
                .build();

        KycDocument saved = kycDocumentRepository.save(document);

        // Update user KYC status to IN_REVIEW
        if (user.getKycStatus() == User.KycStatus.PENDING) {
            user.setKycStatus(User.KycStatus.IN_REVIEW);
            userRepository.save(user);
        }

        // Log the action
        saveAuditLog(user, "DOCUMENT_SUBMITTED",
                user.getKycStatus().name(), "IN_REVIEW", email,
                documentType + " submitted for verification");

        log.info("KYC document submitted. Id: {} for user: {}", saved.getId(), email);
        return mapToResponse(saved);
    }

    // ── Get KYC Status ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<KycDocument> documents = kycDocumentRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        return KycStatusResponse.builder()
                .overallKycStatus(user.getKycStatus().name())
                .documents(documents.stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    // ── Admin: Get Pending Documents ─────────────────────────────
    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getPendingDocuments() {
        return kycDocumentRepository
                .findByVerificationStatusOrderByCreatedAtAsc(
                        KycDocument.VerificationStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Admin: Review KYC Document ───────────────────────────────
    @Transactional
    public KycDocumentResponse reviewDocument(String adminEmail,
                                              KycReviewRequest request) {
        log.info("KYC review by admin: {} for document: {}",
                adminEmail, request.getDocumentId());

        KycDocument document = kycDocumentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Ownership validation — document must belong to the user
        if (!document.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(
                    "Document does not belong to this user");
        }

        String oldStatus = document.getVerificationStatus().name();

        if (request.getAction().equalsIgnoreCase("APPROVE")) {
            document.setVerificationStatus(KycDocument.VerificationStatus.VERIFIED);
            document.setVerifiedAt(LocalDateTime.now());
            document.setVerifiedBy(adminEmail);

            // Update user KYC status to VERIFIED
            user.setKycStatus(User.KycStatus.VERIFIED);
            userRepository.save(user);

            saveAuditLog(user, "KYC_APPROVED", oldStatus, "VERIFIED",
                    adminEmail, request.getRemarks());

            log.info("KYC approved for user: {}", user.getEmail());

        } else if (request.getAction().equalsIgnoreCase("REJECT")) {
            if (request.getRemarks() == null || request.getRemarks().isBlank()) {
                throw new BadRequestException("Rejection reason is required");
            }
            document.setVerificationStatus(KycDocument.VerificationStatus.REJECTED);
            document.setRejectionReason(request.getRemarks());
            document.setVerifiedBy(adminEmail);

            // Update user KYC status to REJECTED
            user.setKycStatus(User.KycStatus.REJECTED);
            userRepository.save(user);

            saveAuditLog(user, "KYC_REJECTED", oldStatus, "REJECTED",
                    adminEmail, request.getRemarks());

            log.info("KYC rejected for user: {}", user.getEmail());

        } else {
            throw new BadRequestException("Invalid action. Use APPROVE or REJECT");
        }

        return mapToResponse(kycDocumentRepository.save(document));
    }

    // ── Get KYC Audit Log ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<KycAuditLog> getAuditLog(Long userId) {
        return kycAuditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ── Private helpers ──────────────────────────────────────────
    private void saveAuditLog(User user, String action,
                              String oldStatus, String newStatus,
                              String performedBy, String remarks) {
        KycAuditLog log = KycAuditLog.builder()
                .user(user)
                .action(action)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .performedBy(performedBy)
                .remarks(remarks)
                .build();
        kycAuditLogRepository.save(log);
    }

    private KycDocumentResponse mapToResponse(KycDocument doc) {
        return KycDocumentResponse.builder()
                .id(doc.getId())
                .userId(doc.getUser().getId())
                .userEmail(doc.getUser().getEmail())
                .userFullName(doc.getUser().getFullName())
                .documentType(doc.getDocumentType().name())
                .documentNumber(doc.getDocumentNumber())
                .documentUrl(doc.getDocumentUrl())
                .verificationStatus(doc.getVerificationStatus().name())
                .rejectionReason(doc.getRejectionReason())
                .verifiedAt(doc.getVerifiedAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}