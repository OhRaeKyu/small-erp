package com.smallerp.common;

import java.util.regex.Pattern;

/** 사업자등록번호(10자리) 정규화와 검증번호 확인 */
public final class BusinessNumber {

    private static final Pattern FORMAT = Pattern.compile("^\\d{3}-?\\d{2}-?\\d{5}$");
    private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    private BusinessNumber() {
    }

    /** 123-45-67890 / 1234567890 형식이면 숫자 10자리로 바꾼다. 형식이 아니면 null. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return FORMAT.matcher(trimmed).matches() ? trimmed.replace("-", "") : null;
    }

    public static boolean isValid(String raw) {
        String digits = normalize(raw);
        if (digits == null) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        sum += ((digits.charAt(8) - '0') * 5) / 10;
        int check = (10 - sum % 10) % 10;
        return check == digits.charAt(9) - '0';
    }

    public static String format(String digits) {
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
    }
}
