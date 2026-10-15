package com.smallerp.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 서버의 비공개 디렉터리에 파일을 저장한다 (웹으로 직접 노출하지 않음, 관리자 API로만 내려줌).
 * 운영에서는 app.storage.dir 을 Docker 볼륨 경로로 지정한다.
 */
@Component
public class LocalFileStorage implements FileStorage {

    private static final Pattern SAFE_KEY = Pattern.compile("^[a-z]+/[0-9a-f-]{36}\\.[a-z]{3,4}$");

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.dir:./data/uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public String save(String folder, String extension, byte[] content) {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + key, e);
        }
        return key;
    }

    @Override
    public Optional<InputStream> open(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.newInputStream(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String key) {
        if (!SAFE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("잘못된 파일 키: " + key);
        }
        return root.resolve(key).normalize();
    }
}
