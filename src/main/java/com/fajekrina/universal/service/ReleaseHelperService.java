package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiRelease;
import com.fajekrina.universal.model.entity.SduiReleasePayloadView;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.response.ReleaseResponse;
import com.fajekrina.universal.model.response.ReleaseResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiReleasePayloadViewRepository;
import com.fajekrina.universal.repository.SduiReleaseRepository;
import com.fajekrina.universal.config.ActorContext;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * Creates a release (shared by publish and rollback): insert → assemble through the view →
 * fail closed on unresolved refs → freeze payload + hash → sign (optional) → activate → audit.
 * Must run inside the caller's transaction (a failure rolls the release row back).
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class ReleaseHelperService {

    private final SduiReleaseRepository releaseRepository;
    private final SduiReleasePayloadViewRepository payloadViewRepository;
    private final SduiPageRepository pageRepository;
    private final MerchantHelperService merchantHelperService;
    private final AuditHelperService auditHelperService;
    private final SignatureService signatureService;

    public ReleaseResponse createRelease(SduiPage page, Long treeId, Long styleId, Long scriptId, Long rollbackOf) {
        int policyVersion = merchantHelperService.latestPolicy(page.getMerchantId())
                .orElseThrow(() -> new UniversalException(UniversalFault.POLICY_NOT_FOUND, "merchant has no policy: " + page.getMerchantId()))
                .getPolicyVersion();
        int sequence = releaseRepository.findFirstByPageIdOrderBySequenceDesc(page.getPageId())
                .map(r -> r.getSequence() + 1).orElse(1);

        SduiRelease release = releaseRepository.saveAndFlush(SduiRelease.builder()
                .pageId(page.getPageId())
                .merchantId(page.getMerchantId())
                .sequence(sequence)
                .treeId(treeId)
                .styleId(styleId)
                .scriptId(scriptId)
                .policyVersion(policyVersion)
                .rollbackOf(rollbackOf)
                .publishedBy(ActorContext.current())
                .publishedAt(ZonedDateTime.now())
                .build());

        SduiReleasePayloadView view = payloadViewRepository.findById(release.getReleaseId()).orElseThrow();
        if (view.getUnresolvedRefs() != null && !Constants.EMPTY_JSON_ARRAY.equals(view.getUnresolvedRefs())) {
            throw new UniversalException(UniversalFault.UNRESOLVED_REFERENCE, "Unresolved references: " + view.getUnresolvedRefs());
        }
        String hash = CanonicalJson.sha256Hex(view.getPayload());
        release.setPayload(view.getPayload());
        release.setContentHash(hash);

        if (signatureService.isEnabled()) {
            try {
                release.setSignature(Base64.getEncoder().encodeToString(signatureService.sign(view.getPayload().getBytes(StandardCharsets.UTF_8))));
                release.setSigningKeyId(signatureService.keyId());
            } catch (Exception e) {
                throw new UniversalException(UniversalFault.SIGNING_FAILED, e.getMessage(), e);
            }
        }
        releaseRepository.saveAndFlush(release);

        page.setActiveReleaseId(release.getReleaseId());
        page.setStatus(PageStatus.PUBLISHED);
        pageRepository.saveAndFlush(page);

        auditHelperService.record(page.getMerchantId(), page.getPageId(), rollbackOf == null ? AuditEvent.PUBLISH : AuditEvent.ROLLBACK,
                release.getReleaseId(), Map.of("sequence", sequence, "policyVersion", policyVersion, "contentHash", hash));
        if (release.getSigningKeyId() != null) {
            auditHelperService.record(page.getMerchantId(), page.getPageId(), AuditEvent.SIGN, release.getReleaseId(),
                    Map.of("keyId", release.getSigningKeyId()));
        }
        log.info("Release {}:{}@{} created (hash {})", page.getMerchantId(), page.getPageKey(), sequence, hash);

        return ReleaseResponseBuilder.builder()
                .message(rollbackOf == null ? Constants.MSG_PAGE_PUBLISHED : Constants.MSG_PAGE_ROLLED_BACK)
                .releaseId(page.getMerchantId() + ":" + page.getPageKey() + "@" + sequence)
                .sequence(sequence)
                .contentHash(hash)
                .nodes(view.getNodeCount())
                .signingKeyId(release.getSigningKeyId())
                .rollbackOf(rollbackOf)
                .build();
    }
}
