package com.fintech.platform.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String mobileNumber;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus;

    @Column
    private String addressStreet;

    @Column
    private String addressCity;

    @Column
    private String addressState;

    @Column
    private String addressPincode;

    @Column
    private String addressCountry;

    @Column
    private String profilePhotoUrl;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Integer failedLoginAttempts;

    @Column
    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Role {
        CUSTOMER,
        ADMIN
    }

    public enum KycStatus {
        PENDING,
        IN_REVIEW,
        VERIFIED,
        REJECTED
    }
}