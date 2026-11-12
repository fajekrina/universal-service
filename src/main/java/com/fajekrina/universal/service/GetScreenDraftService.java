package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiScreen;
import com.fajekrina.universal.model.entity.SduiScreenRelease;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.ScreenKeyRequest;
import com.fajekrina.universal.model.response.ScreenDraftResponse;
import com.fajekrina.universal.model.response.ScreenDraftResponseBuilder;
import com.fajekrina.universal.repository.SduiScreenReleaseRepository;
import com.fajekrina.universal.repository.SduiScreenRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The editable draft of a screen, for the builder. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetScreenDraftService {

    private final SduiScreenRepository screenRepository;
    private final SduiScreenReleaseRepository releaseRepository;

    public ScreenDraftResponse execute(ScreenKeyRequest request) {
        SduiScreen screen = screenRepository.findByMerchantIdAndScreenKey(request.merchantId(), request.screenKey())
                .filter(s -> s.getStatus() != PageStatus.ARCHIVED)
                .orElseThrow(() -> new UniversalException(UniversalFault.SCREEN_NOT_FOUND, "Screen not found: " + request.screenKey()));
        Integer active = screen.getActiveReleaseId() == null ? null
                : releaseRepository.findById(screen.getActiveReleaseId()).map(SduiScreenRelease::getSequence).orElse(null);
        return ScreenDraftResponseBuilder.builder()
                .merchantId(screen.getMerchantId())
                .key(screen.getScreenKey())
                .status(screen.getStatus().name())
                .lockVersion(screen.getVersion())
                .activeSequence(active)
                .payload(CanonicalJson.readMap(screen.getDraft()))
                .build();
    }
}
