package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiRelease;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.PublishedPageRequest;
import com.fajekrina.universal.model.response.PublishedPageResponse;
import com.fajekrina.universal.model.response.PublishedPageResponseBuilder;
import com.fajekrina.universal.model.response.PublishedPageResponseSignedEnvelopeBuilder;
import com.fajekrina.universal.repository.SduiMerchantRepository;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiReleaseRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A1 read: the frozen payload of the active release, byte-for-byte (ETag = content hash,
 * If-None-Match → notModified), optionally as a signed envelope (signs on demand, fill-once).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetPublishedPageService {

    private final SduiMerchantRepository merchantRepository;
    private final SduiPageRepository pageRepository;
    private final SduiReleaseRepository releaseRepository;
    private final SignatureService signatureService;

    public PublishedPageResponse execute(PublishedPageRequest request) {
        SduiRelease release = activeRelease(request);
        if (matches(request.ifNoneMatch(), release.getContentHash())) {
            return PublishedPageResponseBuilder.builder().notModified(true).contentHash(release.getContentHash()).build();
        }
        var response = PublishedPageResponseBuilder.builder().contentHash(release.getContentHash()).payload(release.getPayload());
        if (!request.signed()) return response.build();

        if (!signatureService.isEnabled()) throw new UniversalException(UniversalFault.SIGNING_DISABLED);
        if (release.getSignature() == null) sign(release);
        var payload = CanonicalJson.readMap(release.getPayload());
        return response.envelope(PublishedPageResponseSignedEnvelopeBuilder.builder()
                .schemaVersion(String.valueOf(payload.getOrDefault("schemaVersion", Constants.DEFAULT_SCHEMA_VERSION)))
                .alg(Constants.SIGNING_ALGORITHM)
                .keyId(release.getSigningKeyId())
                .payload(Base64.getEncoder().encodeToString(release.getPayload().getBytes(StandardCharsets.UTF_8)))
                .signature(release.getSignature())
                .build()).build();
    }

    private SduiRelease activeRelease(PublishedPageRequest request) {
        UniversalException notFound = new UniversalException(UniversalFault.PUBLISHED_PAGE_NOT_FOUND,
                "Published universal page not found: " + request.pageKey() + ". It may not exist or is not published.");
        if (!merchantRepository.existsByMerchantIdAndStatus(request.merchantId(), Constants.MERCHANT_ACTIVE)) throw notFound;
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .filter(p -> p.getStatus() != PageStatus.ARCHIVED && p.getActiveReleaseId() != null)
                .orElseThrow(() -> notFound);
        return releaseRepository.findById(page.getActiveReleaseId()).orElseThrow(() -> notFound);
    }

    /** Release published while signing was off: sign now (signature columns are fill-once). */
    private void sign(SduiRelease release) {
        try {
            release.setSignature(Base64.getEncoder().encodeToString(signatureService.sign(release.getPayload().getBytes(StandardCharsets.UTF_8))));
            release.setSigningKeyId(signatureService.keyId());
            releaseRepository.saveAndFlush(release);
        } catch (Exception e) {
            throw new UniversalException(UniversalFault.SIGNING_FAILED, e.getMessage(), e);
        }
    }

    static boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || etag == null) return false;
        for (String part : ifNoneMatch.split(",")) {
            String v = part.trim().replaceFirst("^W/", "").replace("\"", "");
            if (v.equals(etag) || v.equals("*")) return true;
        }
        return false;
    }
}
