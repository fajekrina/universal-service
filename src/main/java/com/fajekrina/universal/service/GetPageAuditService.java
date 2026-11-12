package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.response.AuditListResponse;
import com.fajekrina.universal.model.response.AuditListResponseBuilder;
import com.fajekrina.universal.model.response.AuditListResponseEntryBuilder;
import com.fajekrina.universal.repository.SduiAuditLogRepository;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Audit trail of a page, oldest first. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPageAuditService {

    private final SduiPageRepository pageRepository;
    private final SduiAuditLogRepository auditLogRepository;

    public AuditListResponse execute(PageKeyRequest request) {
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        var entries = auditLogRepository.findAllByPageIdOrderByAuditId(page.getPageId()).stream()
                .map(a -> AuditListResponseEntryBuilder.builder()
                        .event(a.getEvent().name())
                        .releaseId(a.getReleaseId())
                        .actor(a.getCreatedBy())
                        .detail(CanonicalJson.readMap(a.getDetail()))
                        .createdTime(a.getCreatedTime())
                        .build())
                .toList();
        return AuditListResponseBuilder.builder().entries(entries).build();
    }
}
