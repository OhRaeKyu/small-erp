package com.smallerp.storage;

import java.io.InputStream;
import java.util.Optional;

/** 첨부파일 저장소. 지금은 서버 비공개 디스크, 필요하면 다른 구현으로 바꾼다. */
public interface FileStorage {

    /** 저장하고 키를 돌려준다 (예: certificates/uuid.pdf) */
    String save(String folder, String extension, byte[] content);

    Optional<InputStream> open(String key);
}
