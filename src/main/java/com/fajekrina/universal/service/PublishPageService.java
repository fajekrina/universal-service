package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.response.ReleaseResponse;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publish the current draft as a new immutable release (sequence = max + 1). */
@Service
@Transactional
@RequiredArgsConstructor
public class PublishPageService {

    private final SduiPageRepository pageRepository;
    private final ReleaseHelperService releaseHelperService;

    public ReleaseResponse execute(PageKeyRequest request) {
        SduiPage found = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        SduiPage page = pageRepository.findForUpdate(found.getPageId()).orElseThrow();
        if (page.getDraftTreeId() == null) throw new UniversalException(UniversalFault.NO_DRAFT);
        return releaseHelperService.createRelease(page, page.getDraftTreeId(), page.getDraftStyleId(), page.getDraftScriptId(), null);
    }
}
