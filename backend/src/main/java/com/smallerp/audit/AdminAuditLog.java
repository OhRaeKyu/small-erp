package com.smallerp.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 관리자 작업 이력 (A-AUDIT-01). 수정·삭제하지 않는다. */
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    public enum Action {
        APPROVE,
        REJECT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private Long targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> detail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AdminAuditLog() {
    }

    public AdminAuditLog(Long adminId, Action action, String targetType, Long targetId, Map<String, Object> detail) {
        this.adminId = adminId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public Action getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Map<String, Object> getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
