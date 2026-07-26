package com.fintech.platform.kyc.service;

import com.fintech.platform.common.exception.BadRequestException;
import com.fintech.platform.common.exception.ResourceNotFoundException;
import com.fintech.platform.kyc.dto.KycReviewRequest;
import com.fintech.platform.kyc.dto.KycSubmissionRequest;
import com.fintech.platform.kyc.dto.KycDocumentResponse;
import com.fintech.platform.kyc.dto.KycStatusResponse;
import com.fintech.platform.kyc.entity.KycAuditLog;
import com.fintech.platform.kyc.entity.KycDocument;
import com.fintech.platform.kyc.repository.KycAuditLogRepository;
import com.fintech.platform.kyc.repository.KycDocumentRepository;
import com.fintech.platform.user.entity.User;
import com.fintech.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KycService Tests")
class KycServiceTest {

    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private KycAuditLogRepository kycAuditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private KycService kycService;

    private User mockUser;
    private User adminUser;
    private KycDocument mockDocument;
    private KycSubmissionRequest submissionRequest;
    private KycReviewRequest reviewRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).fullName("Test User").email("test@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0)
                .build();

        adminUser = User.builder()
                .id(3L).fullName("Admin").email("admin@fintech.com")
                .role(User.Role.ADMIN).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0)
                .build();

        mockDocument = KycDocument.builder()
                .id(1L).user(mockUser)
                .documentType(KycDocument.DocumentType.AADHAAR)
                .documentNumber("1234-5678-9012")
                .verificationStatus(KycDocument.VerificationStatus.PENDING)
                .build();

        submissionRequest = new KycSubmissionRequest();
        submissionRequest.setDocumentType("AADHAAR");
        submissionRequest.setDocumentNumber("1234-5678-9012");
        submissionRequest.setDocumentUrl("https://storage.fintech.com/kyc/aadhaar.pdf");

        reviewRequest = new KycReviewRequest();
        reviewRequest.setUserId(1L);
        reviewRequest.setDocumentId(1L);
        reviewRequest.setAction("APPROVE");
        reviewRequest.setRemarks("Verified successfully");
    }

    // ── Submit Document Tests ────────────────────────────────────

    @Test
    @DisplayName("Should submit KYC document successfully")
    void shouldSubmitKycDocumentSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(kycDocumentRepository.findByUserIdAndDocumentType(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(mockDocument);
        when(kycAuditLogRepository.save(any(KycAuditLog.class))).thenReturn(null);

        KycDocumentResponse response = kycService
                .submitDocument("test@fintech.com", submissionRequest);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentType()).isEqualTo("AADHAAR");
        assertThat(response.getVerificationStatus()).isEqualTo("PENDING");
        verify(kycDocumentRepository).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid document type")
    void shouldThrowExceptionForInvalidDocumentType() {
        submissionRequest.setDocumentType("INVALID");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() ->
                kycService.submitDocument("test@fintech.com", submissionRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid document type");
    }

    @Test
    @DisplayName("Should throw exception when already verified document resubmitted")
    void shouldThrowExceptionWhenAlreadyVerifiedDocumentResubmitted() {
        mockDocument.setVerificationStatus(KycDocument.VerificationStatus.VERIFIED);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(kycDocumentRepository.findByUserIdAndDocumentType(anyLong(), any()))
                .thenReturn(Optional.of(mockDocument));

        assertThatThrownBy(() ->
                kycService.submitDocument("test@fintech.com", submissionRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already verified");
    }

    // ── Review Document Tests ────────────────────────────────────

    @Test
    @DisplayName("Should approve KYC document successfully")
    void shouldApproveKycDocumentSuccessfully() {
        when(kycDocumentRepository.findById(anyLong()))
                .thenReturn(Optional.of(mockDocument));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(mockDocument);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(kycAuditLogRepository.save(any(KycAuditLog.class))).thenReturn(null);

        KycDocumentResponse response = kycService
                .reviewDocument("admin@fintech.com", reviewRequest);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(kycDocumentRepository).save(any(KycDocument.class));
    }

    @Test
    @DisplayName("Should reject KYC document successfully")
    void shouldRejectKycDocumentSuccessfully() {
        reviewRequest.setAction("REJECT");
        reviewRequest.setRemarks("Document unclear");

        when(kycDocumentRepository.findById(anyLong()))
                .thenReturn(Optional.of(mockDocument));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
        when(kycDocumentRepository.save(any(KycDocument.class))).thenReturn(mockDocument);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(kycAuditLogRepository.save(any(KycAuditLog.class))).thenReturn(null);

        KycDocumentResponse response = kycService
                .reviewDocument("admin@fintech.com", reviewRequest);

        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when document does not belong to user")
    void shouldThrowExceptionWhenDocumentDoesNotBelongToUser() {
        User otherUser = User.builder()
                .id(2L).fullName("Other User").email("other@fintech.com")
                .role(User.Role.CUSTOMER).isActive(true)
                .kycStatus(User.KycStatus.PENDING)
                .failedLoginAttempts(0)
                .build();

        mockDocument.setUser(otherUser);

        when(kycDocumentRepository.findById(anyLong()))
                .thenReturn(Optional.of(mockDocument));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() ->
                kycService.reviewDocument("admin@fintech.com", reviewRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Document does not belong to this user");
    }

    @Test
    @DisplayName("Should throw exception for invalid review action")
    void shouldThrowExceptionForInvalidReviewAction() {
        reviewRequest.setAction("INVALID");

        when(kycDocumentRepository.findById(anyLong()))
                .thenReturn(Optional.of(mockDocument));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() ->
                kycService.reviewDocument("admin@fintech.com", reviewRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid action. Use APPROVE or REJECT");
    }

    // ── Get KYC Status Tests ─────────────────────────────────────

    @Test
    @DisplayName("Should return KYC status with documents")
    void shouldReturnKycStatusWithDocuments() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(kycDocumentRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(mockDocument));

        KycStatusResponse response = kycService.getKycStatus("test@fintech.com");

        assertThat(response).isNotNull();
        assertThat(response.getOverallKycStatus()).isEqualTo("PENDING");
        assertThat(response.getDocuments()).hasSize(1);
    }
}