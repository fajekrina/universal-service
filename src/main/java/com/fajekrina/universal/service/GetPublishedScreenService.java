package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiScreenRelease;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.PublishedScreenRequest;
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

/** Client read of a spec screen: the active release's frozen bytes (ETag), or its signed envelope. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPublishedScreenService {

    private final SduiMerchantRepository merchantRepository;
    private final SduiScreenRepository screenRepository;
    private final SduiScreenReleaseRepository releaseRepository;
    private final ScreenEnvelopeHelperService envelopeHelperService;

    public PublishedPageResponse execute(PublishedScreenRequest request) {
        UniversalException notFound = new UniversalException(UniversalFault.PUBLISHED_SCREEN_NOT_FOUND,
                "Published screen not found: " + request.screenKey());
        if (!merchantRepository.existsByMerchantIdAndStatus(request.merchantId(), Constants.MERCHANT_ACTIVE)) throw notFound;
        SduiScreenRelease release = screenRepository.findByMerchantIdAndScreenKey(request.merchantId(), request.screenKey())
                .filter(s -> s.getStatus() != PageStatus.ARCHIVED && s.getActiveReleaseId() != null)
                .flatMap(s -> releaseRepository.findById(s.getActiveReleaseId()))
                .orElseThrow(() -> notFound);
        String schemaVersion = String.valueOf(CanonicalJson.readMap(release.getPayload()).get("schemaVersion"));
        return envelopeHelperService.respond(release.getPayload(), schemaVersion, request.signed(), request.ifNoneMatch(),
                new String[] { release.getSignature(), release.getSigningKeyId() });
    }
}
