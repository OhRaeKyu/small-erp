package com.smallerp.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 업종 팩. 1단계에서는 코드와 이름만 쓴다 (기본 데이터 복사는 승인 기능에서). */
@Entity
@Table(name = "industry_packs")
public class IndustryPack {

    @Id
    private String code;

    private String name;

    protected IndustryPack() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
