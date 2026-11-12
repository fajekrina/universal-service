package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiRelease;
import com.fajekrina.universal.model.request.RollbackPageRequest;
import com.fajekrina.universal.model.response.ReleaseResponse;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiReleaseRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rollback = a NEW release with the refs of an old one (sequence keeps rising, latest policy). */
@Service
@Transactional
@RequiredArgsConstructor
public class RollbackPageService {

    private final SduiPageRepository pageRepository;
    private final SduiReleaseRepository releaseRepository;
    private final ReleaseHelperService releaseHelperService;

    public ReleaseResponse execute(RollbackPageRequest request) {
        SduiPage found = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        SduiPage page = pageRepository.findForUpdate(found.getPageId()).orElseThrow();
        SduiRelease old = releaseRepository.findByPageIdAndSequence(page.getPageId(), request.toSequence())
                .orElseThrow(() -> new UniversalException(UniversalFault.RELEASE_NOT_FOUND, "release #" + request.toSequence() + " not found"));
        return releaseHelperService.createRelease(page, old.getTreeId(), old.getStyleId(), old.getScriptId(), old.getReleaseId());
    }
}
