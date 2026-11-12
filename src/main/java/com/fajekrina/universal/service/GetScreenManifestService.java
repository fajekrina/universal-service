package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiScreen;
import com.fajekrina.universal.model.entity.SduiScreenRelease;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.ScreenManifestRequest;
import com.fajekrina.universal.model.response.PublishedPageResponse;
import com.fajekrina.universal.repository.SduiMerchantRepository;
import com.fajekrina.universal.repository.SduiScreenReleaseRepository;
import com.fajekrina.universal.repository.SduiScreenRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The merchant's version manifest {sequence, screens: {key: "v<seq>"}} for the client cache.
 * {@code sequence} = the merchant's highest screen release id: it only grows (releases are
 * never deleted), so the client's anti-rollback check holds. Signed like a screen.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetScreenManifestService {

    private final SduiMerchantRepository merchantRepository;
    private final SduiScreenRepository screenRepository;
    private final SduiScreenReleaseRepository releaseRepository;
    private final ScreenEnvelopeHelperService envelopeHelperService;

    public PublishedPageResponse execute(ScreenManifestRequest request) {
        if (!merchantRepository.existsByMerchantIdAndStatus(request.merchantId(), Constants.MERCHANT_ACTIVE)) {
            throw new UniversalException(UniversalFault.PUBLISHED_SCREEN_NOT_FOUND, "Unknown merchant: " + request.merchantId());
        }
        var screens = screenRepository.findAllByMerchantIdAndStatusNotOrderByScreenKey(request.merchantId(), PageStatus.ARCHIVED);
        Map<Long, SduiScreenRelease> releases = new HashMap<>();
        releaseRepository.findAllByReleaseIdIn(screens.stream().map(SduiScreen::getActiveReleaseId).filter(Objects::nonNull).toList())
                .forEach(r -> releases.put(r.getReleaseId(), r));
        Map<String, String> versions = new TreeMap<>();
        for (SduiScreen s : screens) {
            SduiScreenRelease r = s.getActiveReleaseId() == null ? null : releases.get(s.getActiveReleaseId());
            if (r != null) versions.put(s.getScreenKey(), Constants.SCREEN_VERSION_PREFIX + r.getSequence());
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("sequence", releaseRepository.maxReleaseId(request.merchantId()).orElse(0L));
        manifest.put("screens", versions);
        return envelopeHelperService.respond(CanonicalJson.canonical(manifest), Constants.SPEC_SCHEMA_VERSION,
                request.signed(), request.ifNoneMatch(), null);
    }
}
