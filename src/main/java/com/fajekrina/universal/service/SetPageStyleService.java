package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiStyle;
import com.fajekrina.universal.model.request.SetPageStyleRequest;
import com.fajekrina.universal.model.response.DraftPageResponse;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiStyleRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Select the style artefact used by a page draft (latest or a fixed version; null key = none). */
@Service
@Transactional
@RequiredArgsConstructor
public class SetPageStyleService {

    private final SduiPageRepository pageRepository;
    private final SduiStyleRepository styleRepository;
    private final DraftHelperService draftHelperService;

    public DraftPageResponse execute(SetPageStyleRequest request) {
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        Long styleId = null;
        if (request.styleKey() != null && !request.styleKey().isBlank()) {
            SduiStyle style = (request.version() == null
                    ? styleRepository.findFirstByMerchantIdAndStyleKeyOrderByVersionDesc(request.merchantId(), request.styleKey())
                    : styleRepository.findByMerchantIdAndStyleKeyAndVersion(request.merchantId(), request.styleKey(), request.version()))
                    .orElseThrow(() -> new UniversalException(UniversalFault.STYLE_NOT_FOUND,
                            "style not found: " + request.styleKey() + (request.version() == null ? "" : "@" + request.version())));
            styleId = style.getStyleId();
        }
        page.setDraftStyleId(styleId);
        page = pageRepository.saveAndFlush(page);
        if (page.getDraftTreeId() == null) throw new UniversalException(UniversalFault.NO_DRAFT);
        return draftHelperService.toDraftResponse(page);
    }
}
