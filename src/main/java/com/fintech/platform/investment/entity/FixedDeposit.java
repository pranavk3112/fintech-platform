package com.fintech.platform.investment.entity;

import com.fintech.platform.user.entity.User;
import com.fintech.platform.wallet.entity.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_deposits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FixedDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, unique = true)
    private String fdNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestType interestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompoundingFrequency compoundingFrequency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maturityAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal interestEarned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FdStatus status;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal prematureWithdrawalPenalty;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum InterestType {
        SIMPLE,
        COMPOUND
    }

    public enum CompoundingFrequency {
        MONTHLY,
        QUARTERLY,
        HALF_YEARLY,
        YEARLY
    }

    public enum FdStatus {
        ACTIVE,
        MATURED,
        CLOSED,
        PREMATURE_CLOSED
    }
}