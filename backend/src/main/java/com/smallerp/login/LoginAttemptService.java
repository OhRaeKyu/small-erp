package com.smallerp.login;

import com.smallerp.common.ApiException;
import com.smallerp.common.ErrorCode;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연속 로그인 실패를 세고 잠근다. 로그인 실패는 예외로 끝나 바깥 트랜잭션이 롤백되므로
 * 실패 기록은 별도 트랜잭션(REQUIRES_NEW)으로 남긴다. 동시에 여러 번 틀려도 한 문장 UPDATE로 정확히 센다.
 */
@Service
public class LoginAttemptService {

    /** 잠금 컬럼을 가진 계정 테이블 (사업자 users, 관리자 platform_admins) */
    public enum Account {
        USER("users"),
        ADMIN("platform_admins");

        private final String table;

        Account(String table) {
            this.table = table;
        }
    }

    private final JdbcTemplate jdbc;
    private final LoginLockProperties props;

    public LoginAttemptService(JdbcTemplate jdbc, LoginLockProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    /** 잠겨 있으면 비밀번호를 확인하기 전에 막는다 */
    public void checkNotLocked(Instant lockedUntil) {
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            throw new ApiException(ErrorCode.LOGIN_LOCKED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Account account, Long id) {
        // 실패 횟수가 한도에 닿으면 잠그고 횟수는 0으로 되돌린다 (잠금이 풀린 뒤 다시 5번 기회)
        jdbc.update("""
                update %s set
                    locked_until = case when failed_login_count + 1 >= ? then ? else locked_until end,
                    failed_login_count = case when failed_login_count + 1 >= ? then 0 else failed_login_count + 1 end
                where id = ?
                """.formatted(account.table),
                props.maxFailures(), Timestamp.from(Instant.now().plus(props.lockDuration())),
                props.maxFailures(), id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Account account, Long id) {
        jdbc.update("update %s set failed_login_count = 0, locked_until = null where id = ? and (failed_login_count > 0 or locked_until is not null)"
                .formatted(account.table), id);
    }
}
