package com.fajekrina.universal.service;

import com.fajekrina.universal.model.response.PublishedPageResponse;
import com.fajekrina.universal.model.response.PublishedPageResponseBuilder;
import com.fajekrina.universal.model.response.PublishedPageResponseSignedEnvelopeBuilder;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Serves frozen bytes the way the client engine expects: bare (development) or as the
 * Ed25519 envelope {schemaVersion, alg, keyId, payload: base64(bytes), signature},
 * with ETag = sha256(bytes) and If-None-Match → notModified.
 */
@Service
@RequiredArgsConstructor
public class ScreenEnvelopeHelperService {

    private final SignatureService signatureService;

    /** {signature (base64), keyId} over {@code bytes}; requires signing to be enabled. */
    public String[] sign(String bytes) {
        if (!signatureService.isEnabled()) throw new UniversalException(UniversalFault.SIGNING_DISABLED);
        try {
            return new String[] {
                Base64.getEncoder().encodeToString(signatureService.sign(bytes.getBytes(StandardCharsets.UTF_8))),
                signatureService.keyId()
            };
        } catch (Exception e) {
            throw new UniversalException(UniversalFault.SIGNING_FAILED, e.getMessage(), e);
        }
    }

    /** Signs at write time when signing is on; {@code null} otherwise. */
    public String[] signIfEnabled(String bytes) {
        return signatureService.isEnabled() ? sign(bytes) : null;
    }

    /**
     * @param stored {signature, keyId} stored with the bytes, or {@code null}/empty: signed on
     *               demand (Ed25519 is deterministic, so every response carries the same signature)
     */
    public PublishedPageResponse respond(String bytes, String schemaVersion, boolean signed, String ifNoneMatch, String[] stored) {
        String hash = CanonicalJson.sha256Hex(bytes);
        if (GetPublishedPageService.matches(ifNoneMatch, hash)) {
            return PublishedPageResponseBuilder.builder().notModified(true).contentHash(hash).build();
        }
        var response = PublishedPageResponseBuilder.builder().contentHash(hash).payload(bytes);
        if (!signed) return response.build();
        String[] signature = stored != null && stored[0] != null ? stored : sign(bytes);
        return response.envelope(PublishedPageResponseSignedEnvelopeBuilder.builder()
                .schemaVersion(schemaVersion == null ? Constants.DEFAULT_SCHEMA_VERSION : schemaVersion)
                .alg(Constants.SIGNING_ALGORITHM)
                .keyId(signature[1])
                .payload(Base64.getEncoder().encodeToString(bytes.getBytes(StandardCharsets.UTF_8)))
                .signature(signature[0])
                .build()).build();
    }
}
