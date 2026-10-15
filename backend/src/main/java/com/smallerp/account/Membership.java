package com.smallerp.account;

import com.smallerp.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "memberships")
public class Membership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role;

    protected Membership() {
    }

    public Membership(Tenant tenant, User user, MembershipRole role) {
        this.tenant = tenant;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public User getUser() {
        return user;
    }

    public MembershipRole getRole() {
        return role;
    }
}
