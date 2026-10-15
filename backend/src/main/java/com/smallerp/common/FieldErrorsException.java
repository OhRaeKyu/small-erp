package com.smallerp.common;

import java.util.Map;

/** Bean Validation으로 표현하기 어려운 입력 오류를 필드별 메시지와 함께 400으로 돌려준다 */
public class FieldErrorsException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public FieldErrorsException(Map<String, String> fieldErrors) {
        super(ErrorCode.INVALID_INPUT.message());
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
