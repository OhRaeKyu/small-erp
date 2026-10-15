package com.smallerp.common;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, String> fieldErrors) {

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), code.message(), Map.of());
    }
}
