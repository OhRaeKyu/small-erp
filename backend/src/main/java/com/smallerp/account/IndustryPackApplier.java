package com.smallerp.account;

import org.springframework.stereotype.Component;

/**
 * 승인 시 업종 팩 기본 데이터를 사업장에 적용한다 (U-AUTH-03).
 * 지금은 품목 분류만 사업장 설정에 복사한다. 품목·FAQ 테이블이 생기는 단계(3, 6)에서 여기에 이어 붙인다.
 */
@Component
public class IndustryPackApplier {

    private final IndustryPackRepository industryPacks;

    public IndustryPackApplier(IndustryPackRepository industryPacks) {
        this.industryPacks = industryPacks;
    }

    public void apply(Tenant tenant) {
        IndustryPack pack = industryPacks.findById(tenant.getIndustryCode())
                .orElseThrow(() -> new IllegalStateException("업종 팩이 없습니다: " + tenant.getIndustryCode()));
        tenant.putSetting("itemCategories", pack.getItemCategories());
    }
}
