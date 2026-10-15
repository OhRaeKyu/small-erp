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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /** 요일별 영업시간 (월~일 7개) */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<BusinessHour> businessHours = new ArrayList<>();

    /** 기본 하루 생산 가능량. 품목별 값이 없을 때 쓴다 */
    private Integer defaultDailyCapacity;

    /** 픽업 N일 전까지만 주문을 받는다 */
    private Integer orderLeadDays;

    private String address;

    private String phone;

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

    public void updateProfile(String name, String address, String phone, List<BusinessHour> businessHours,
                              Integer defaultDailyCapacity, Integer orderLeadDays) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.businessHours = new ArrayList<>(businessHours);
        this.defaultDailyCapacity = defaultDailyCapacity;
        this.orderLeadDays = orderLeadDays;
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

    public List<BusinessHour> getBusinessHours() {
        return businessHours == null ? List.of() : businessHours;
    }

    public Integer getDefaultDailyCapacity() {
        return defaultDailyCapacity;
    }

    public Integer getOrderLeadDays() {
        return orderLeadDays;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
