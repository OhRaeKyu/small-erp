package com.smallerp.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    List<AdminAuditLog> findByTargetTypeAndTargetIdOrderByIdDesc(String targetType, Long targetId);
}
