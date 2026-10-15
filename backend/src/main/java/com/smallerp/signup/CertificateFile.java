package com.smallerp.signup;

import com.smallerp.common.ApiException;
import com.smallerp.common.ErrorCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import org.springframework.web.multipart.MultipartFile;

/** 사업자등록증 첨부: jpg·png·pdf, 5MB 이하. 확장자가 아니라 파일 앞부분(시그니처)으로 형식을 확인한다. */
public enum CertificateFile {
    JPG("jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'}),
    PDF("pdf", "application/pdf", new byte[] {'%', 'P', 'D', 'F'});

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private final String extension;
    private final String contentType;
    private final byte[] magic;

    CertificateFile(String extension, String contentType, byte[] magic) {
        this.extension = extension;
        this.contentType = contentType;
        this.magic = magic;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    public static CertificateFile detect(byte[] content) {
        for (CertificateFile type : values()) {
            if (content.length >= type.magic.length
                    && Arrays.equals(Arrays.copyOf(content, type.magic.length), type.magic)) {
                return type;
            }
        }
        throw new ApiException(ErrorCode.INVALID_FILE_TYPE);
    }

    public static CertificateFile fromKey(String key) {
        String ext = key.substring(key.lastIndexOf('.') + 1);
        return Arrays.stream(values()).filter(t -> t.extension.equals(ext)).findFirst().orElse(PDF);
    }

    static byte[] read(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(ErrorCode.FILE_TOO_LARGE);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
