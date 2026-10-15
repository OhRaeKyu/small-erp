package com.smallerp.account;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("select m from Membership m join fetch m.tenant where m.user.id = :userId order by m.id")
    List<Membership> findWithTenantByUserId(Long userId);

    @Query("select m from Membership m join fetch m.tenant join fetch m.user "
            + "where m.user.id = :userId and m.tenant.id = :tenantId")
    Optional<Membership> findWithTenantAndUser(Long userId, Long tenantId);
}
