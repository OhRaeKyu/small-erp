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
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String businessNo;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String industryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(nullable = false)
    private String plan = "FREE";

    /** 업종 팩에서 복사한 기본값 등 사업장별 설정 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> settings = new HashMap<>();

    protected Tenant() {
    }

    public Tenant(String businessNo, String name, String industryCode) {
        this.businessNo = businessNo;
        this.name = name;
        this.industryCode = industryCode;
        this.status = TenantStatus.ACTIVE;
    }

    public void putSetting(String key, Object value) {
        Map<String, Object> next = new HashMap<>(settings);
        next.put(key, value);
        this.settings = next;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    public Long getId() {
        return id;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public String getName() {
        return name;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public TenantStatus getStatus() {
        return status;
    }
}
