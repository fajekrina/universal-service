package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiAuditLog;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.repository.SduiAuditLogRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Append-only audit trail (actor = ActorContext via JPA auditing). */
@Service
@RequiredArgsConstructor
public class AuditHelperService {

    private final SduiAuditLogRepository auditLogRepository;

    public void record(String merchantId, String pageId, AuditEvent event, Long releaseId, Map<String, Object> detail) {
        auditLogRepository.save(SduiAuditLog.builder()
                .merchantId(merchantId)
                .pageId(pageId)
                .event(event)
                .releaseId(releaseId)
                .detail(CanonicalJson.write(detail == null ? Map.of() : detail))
                .build());
    }
}
