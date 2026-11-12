package com.fajekrina.universal.repository;

import com.fajekrina.universal.model.entity.SduiAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SduiAuditLogRepository extends JpaRepository<SduiAuditLog, Long> {
    List<SduiAuditLog> findAllByPageIdOrderByAuditId(String pageId);
}
