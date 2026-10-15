package com.smallerp.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 업종 팩. 승인 시 사업장에 기본값(품목 분류 등)을 복사한다. */
@Entity
@Table(name = "industry_packs")
public class IndustryPack {

    @Id
    private String code;

    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> itemCategories;

    protected IndustryPack() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public List<String> getItemCategories() {
        return itemCategories;
    }
}
