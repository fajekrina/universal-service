package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.response.ArchivePageResponse;
import com.fajekrina.universal.model.response.ArchivePageResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;

/** DELETE = archive: releases stay as immutable history, the client endpoint stops serving the page. */
@Service
@Transactional
@RequiredArgsConstructor
public class ArchivePageService {

    private final SduiPageRepository pageRepository;
    private final AuditHelperService auditHelperService;

    public ArchivePageResponse execute(PageKeyRequest request) {
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .filter(p -> p.getStatus() != PageStatus.ARCHIVED)
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        page.setStatus(PageStatus.ARCHIVED);
        page.setArchivedTime(ZonedDateTime.now());
        pageRepository.saveAndFlush(page);
        auditHelperService.record(request.merchantId(), page.getPageId(), AuditEvent.ARCHIVE, null, Map.of());
        return ArchivePageResponseBuilder.builder().message(Constants.MSG_PAGE_ARCHIVED).pageKey(request.pageKey()).build();
    }
}
