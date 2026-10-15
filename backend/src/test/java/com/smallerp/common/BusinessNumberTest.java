package com.smallerp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BusinessNumberTest {

    @ParameterizedTest
    @ValueSource(strings = {"1234567891", "123-45-67891", " 214-87-00124 ", "3141592650"})
    void 검증번호가_맞으면_통과한다(String raw) {
        assertThat(BusinessNumber.isValid(raw)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "123-45-6789", "12-345-67891", "abcdefghij", ""})
    void 형식이나_검증번호가_틀리면_막는다(String raw) {
        assertThat(BusinessNumber.isValid(raw)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void 하이픈을_빼고_다시_넣는다() {
        assertThat(BusinessNumber.normalize("123-45-67891")).isEqualTo("1234567891");
        assertThat(BusinessNumber.normalize("owner@test.com")).isNull();
        assertThat(BusinessNumber.format("1234567891")).isEqualTo("123-45-67891");
    }
}
