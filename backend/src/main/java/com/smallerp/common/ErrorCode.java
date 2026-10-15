package com.smallerp.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    INVALID_BUSINESS_NO(HttpStatus.BAD_REQUEST, "올바른 사업자등록번호가 아닙니다."),
    UNKNOWN_INDUSTRY(HttpStatus.BAD_REQUEST, "선택할 수 없는 업종입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    SIGNUP_PENDING(HttpStatus.FORBIDDEN, "회원가입을 검토하고 있습니다. 승인되면 로그인할 수 있습니다."),
    SIGNUP_REJECTED(HttpStatus.FORBIDDEN, "회원가입이 반려되었습니다. 가입 상태 조회에서 사유를 확인해 주세요."),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "잠긴 계정입니다. 관리자에게 문의해 주세요."),
    TENANT_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 정지된 사업장입니다. 관리자에게 문의해 주세요."),
    NO_MEMBERSHIP(HttpStatus.FORBIDDEN, "소속된 사업장이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 처리된 회원가입 요청입니다."),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 사업자등록번호입니다."),
    SIGNUP_ALREADY_PENDING(HttpStatus.CONFLICT, "이 사업자등록번호로 검토 중인 회원가입이 있습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
