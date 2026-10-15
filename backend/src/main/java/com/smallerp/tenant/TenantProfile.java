package com.smallerp.tenant;

import com.smallerp.account.BusinessHour;
import com.smallerp.account.Tenant;
import com.smallerp.common.BusinessNumber;
import java.util.List;

/** 사업장 정보 화면(U-080)에 보여 주는 값. 사업자번호·업종은 읽기 전용 */
public record TenantProfile(
        String businessNo,
        String industryCode,
        String industryName,
        String name,
        String address,
        String phone,
        List<BusinessHour> businessHours,
        Integer defaultDailyCapacity,
        Integer orderLeadDays) {

    static TenantProfile of(Tenant t, String industryName) {
        return new TenantProfile(BusinessNumber.format(t.getBusinessNo()), t.getIndustryCode(), industryName,
                t.getName(), t.getAddress(), t.getPhone(), TenantSettingsService.withAllDays(t.getBusinessHours()),
                t.getDefaultDailyCapacity(), t.getOrderLeadDays());
    }
}
