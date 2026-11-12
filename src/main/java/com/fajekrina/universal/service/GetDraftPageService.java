package com.fajekrina.universal.service;

import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.response.DraftPageResponse;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Draft of a page for the builder (assembled from the draft artefacts). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDraftPageService {

    private final SduiPageRepository pageRepository;
    private final DraftHelperService draftHelperService;

    public DraftPageResponse execute(PageKeyRequest request) {
        return pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .filter(p -> p.getDraftTreeId() != null)
                .map(draftHelperService::toDraftResponse)
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
    }
}
