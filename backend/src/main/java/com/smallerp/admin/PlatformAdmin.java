package com.smallerp.admin;

import com.smallerp.common.BaseTimeEntity;
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
@Table(name = "platform_admins")
public class PlatformAdmin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformAdminRole role;

    /** 로그인 잠금 해제 시각. 값 변경은 LoginAttemptService가 SQL로 한다. */
    @Column(insertable = false, updatable = false)
    private Instant lockedUntil;

    protected PlatformAdmin() {
    }

    public PlatformAdmin(String email, String passwordHash, String name, PlatformAdminRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public PlatformAdminRole getRole() {
        return role;
    }
}
