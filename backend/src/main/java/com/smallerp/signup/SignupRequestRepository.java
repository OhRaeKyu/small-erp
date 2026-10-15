package com.smallerp.signup;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SignupRequestRepository
        extends JpaRepository<SignupRequest, Long>, JpaSpecificationExecutor<SignupRequest> {

    Optional<SignupRequest> findFirstByBusinessNoOrderByIdDesc(String businessNo);

    /** 승인·반려는 행 잠금으로 직렬화한다: 두 관리자가 동시에 눌러도 한 번만 처리된다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SignupRequest r where r.id = :id")
    Optional<SignupRequest> findByIdForUpdate(Long id);

    List<SignupRequest> findByBusinessNoOrderByIdDesc(String businessNo);

    boolean existsByBusinessNoAndStatus(String businessNo, SignupStatus status);

    @Query("select r.status as status, count(r) as count from SignupRequest r group by r.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        SignupStatus getStatus();

        long getCount();
    }
}
