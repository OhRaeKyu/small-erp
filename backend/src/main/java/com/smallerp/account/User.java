package com.smallerp.account;

import com.smallerp.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 사업장 사용자. 대표는 login_id = 사업자번호, 직원은 login_id = 이메일. */
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    protected User() {
    }

    public User(String loginId, String email, String passwordHash, String name) {
        this.loginId = loginId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.status = UserStatus.ACTIVE;
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public Long getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
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

    public UserStatus getStatus() {
        return status;
    }
}
