package com.smallerp.signup;

import com.smallerp.common.ApiException;
import com.smallerp.common.BaseTimeEntity;
import com.smallerp.common.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "signup_requests")
public class SignupRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String businessNo;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String industryCode;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String email;

    /** 승인 시 대표 계정으로 옮긴다 */
    @Column(nullable = false)
    private String passwordHash;

    private String certificateFileKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignupStatus status;

    private String rejectReason;

    private Long reviewedBy;

    private Instant reviewedAt;

    private Long tenantId;

    protected SignupRequest() {
    }

    public SignupRequest(String businessNo, String companyName, String ownerName, String industryCode,
                         String phone, String email, String passwordHash) {
        this.businessNo = businessNo;
        this.companyName = companyName;
        this.ownerName = ownerName;
        this.industryCode = industryCode;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = SignupStatus.PENDING;
    }

    public void markApproved(Long adminId, Long tenantId) {
        requirePending();
        this.status = SignupStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
        this.tenantId = tenantId;
    }

    public void markRejected(Long adminId, String reason) {
        requirePending();
        this.status = SignupStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
        this.rejectReason = reason;
    }

    public void attachCertificate(String fileKey) {
        this.certificateFileKey = fileKey;
    }

    private void requirePending() {
        if (status != SignupStatus.PENDING) {
            throw new ApiException(ErrorCode.ALREADY_REVIEWED);
        }
    }

    public Long getId() {
        return id;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getCertificateFileKey() {
        return certificateFileKey;
    }

    public SignupStatus getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
