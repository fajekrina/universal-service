package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiScreen;
import com.fajekrina.universal.model.entity.SduiScreenRelease;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.ScreenKeyRequest;
import com.fajekrina.universal.model.response.ScreenReleaseResponse;
import com.fajekrina.universal.model.response.ScreenReleaseResponseBuilder;
import com.fajekrina.universal.repository.SduiScreenReleaseRepository;
import com.fajekrina.universal.repository.SduiScreenRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.SpecScreenValidator;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publish the draft: re-validate, freeze the bytes as release {@code sequence = last + 1}
 * (signed when signing is on), activate it. Publishing an unchanged draft is a no-op.
 */
@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class PublishScreenService {

    private final SduiScreenRepository screenRepository;
    private final SduiScreenReleaseRepository releaseRepository;
    private final ScreenEnvelopeHelperService envelopeHelperService;
    private final AuditHelperService auditHelperService;

    public ScreenReleaseResponse execute(ScreenKeyRequest request) {
        SduiScreen screen = screenRepository.findForUpdate(request.merchantId(), request.screenKey())
                .filter(s -> s.getStatus() != PageStatus.ARCHIVED)
                .orElseThrow(() -> new UniversalException(UniversalFault.SCREEN_NOT_FOUND, "Screen not found: " + request.screenKey()));
        SpecScreenValidator.validate(CanonicalJson.readMap(screen.getDraft()), screen.getScreenKey());
        String bytes = screen.getDraft();
        String hash = CanonicalJson.sha256Hex(bytes);

        SduiScreenRelease active = screen.getActiveReleaseId() == null ? null : releaseRepository.findById(screen.getActiveReleaseId()).orElse(null);
        if (active != null && active.getContentHash().equals(hash)) {
            return response(Constants.MSG_SCREEN_ALREADY_PUBLISHED, active, true, screen.getVersion());
        }
        int sequence = releaseRepository.findFirstByMerchantIdAndScreenKeyOrderBySequenceDesc(screen.getMerchantId(), screen.getScreenKey())
                .map(r -> r.getSequence() + 1).orElse(1);
        String[] signature = envelopeHelperService.signIfEnabled(bytes);
        SduiScreenRelease release = releaseRepository.saveAndFlush(SduiScreenRelease.builder()
                .merchantId(screen.getMerchantId())
                .screenKey(screen.getScreenKey())
                .sequence(sequence)
                .payload(bytes)
                .contentHash(hash)
                .signature(signature == null ? null : signature[0])
                .signingKeyId(signature == null ? null : signature[1])
                .build());
        screen.setActiveReleaseId(release.getReleaseId());
        screen.setStatus(PageStatus.PUBLISHED);
        screen = screenRepository.saveAndFlush(screen);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("screenReleaseId", release.getReleaseId());
        detail.put("sequence", sequence);
        detail.put("contentHash", hash);
        if (signature != null) detail.put("keyId", signature[1]);
        auditHelperService.record(screen.getMerchantId(), Constants.SCREEN_AUDIT_PREFIX + screen.getScreenKey(), AuditEvent.PUBLISH, null, detail);
        log.info("Screen {}:{}@{} published (hash {})", screen.getMerchantId(), screen.getScreenKey(), sequence, hash);
        return response(Constants.MSG_SCREEN_PUBLISHED, release, false, screen.getVersion());
    }

    private static ScreenReleaseResponse response(String message, SduiScreenRelease release, boolean unchanged, Integer lockVersion) {
        return ScreenReleaseResponseBuilder.builder()
                .message(message)
                .key(release.getScreenKey())
                .sequence(release.getSequence())
                .contentHash(release.getContentHash())
                .signingKeyId(release.getSigningKeyId())
                .unchanged(unchanged)
                .lockVersion(lockVersion)
                .build();
    }
}
