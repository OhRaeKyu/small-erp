package com.smallerp.signup;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SignupRequestRepository
        extends JpaRepository<SignupRequest, Long>, JpaSpecificationExecutor<SignupRequest> {

    Optional<SignupRequest> findFirstByBusinessNoOrderByIdDesc(String businessNo);

    boolean existsByBusinessNoAndStatus(String businessNo, SignupStatus status);

    @Query("select r.status as status, count(r) as count from SignupRequest r group by r.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        SignupStatus getStatus();

        long getCount();
    }
}
