package com.smallerp.login;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 로그인 잠금 정책: maxFailures 번 연속 실패하면 lockDuration 동안 잠근다 */
@ConfigurationProperties(prefix = "app.login")
public record LoginLockProperties(@DefaultValue("5") int maxFailures, @DefaultValue("PT10M") Duration lockDuration) {
}
