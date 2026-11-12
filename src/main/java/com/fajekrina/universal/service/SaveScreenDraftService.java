package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiScreen;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.SaveScreenRequest;
import com.fajekrina.universal.model.response.SaveScreenResponse;
import com.fajekrina.universal.model.response.SaveScreenResponseBuilder;
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

import java.util.Map;

/**
 * Save a spec screen draft: schema-validated, stored canonically (sorted keys), a no-op when
 * the content is unchanged, optimistic lock via {@code lockVersion} (stale → 409 SDUI-004).
 */
@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class SaveScreenDraftService {

    private final SduiScreenRepository screenRepository;
    private final MerchantHelperService merchantHelperService;
    private final AuditHelperService auditHelperService;

    public SaveScreenResponse execute(SaveScreenRequest request) {
        var payload = SpecScreenValidator.validate(request.payload(), request.screenKey());
        merchantHelperService.ensureMerchant(request.merchantId());
        String draft = CanonicalJson.canonical(payload);
        String hash = CanonicalJson.sha256Hex(draft);

        SduiScreen screen = screenRepository.findByMerchantIdAndScreenKey(request.merchantId(), request.screenKey()).orElse(null);
        if (screen != null) {
            if (request.lockVersion() != null && !request.lockVersion().equals(screen.getVersion())) {
                throw new UniversalException(UniversalFault.LOCK_CONFLICT,
                        "lockVersion " + request.lockVersion() + " is stale (current " + screen.getVersion() + ")");
            }
            if (hash.equals(screen.getDraftHash()) && screen.getStatus() != PageStatus.ARCHIVED) {
                return response(Constants.MSG_SCREEN_UNCHANGED, screen, true);
            }
            screen.setDraft(draft);
            screen.setDraftHash(hash);
            if (screen.getStatus() == PageStatus.ARCHIVED) {
                screen.setStatus(screen.getActiveReleaseId() == null ? PageStatus.DRAFT : PageStatus.PUBLISHED);
            }
        } else {
            screen = SduiScreen.builder()
                    .merchantId(request.merchantId())
                    .screenKey(request.screenKey())
                    .status(PageStatus.DRAFT)
                    .draft(draft)
                    .draftHash(hash)
                    .build();
        }
        screen = screenRepository.saveAndFlush(screen);
        auditHelperService.record(request.merchantId(), Constants.SCREEN_AUDIT_PREFIX + request.screenKey(), AuditEvent.SAVE_DRAFT, null,
                Map.of("draftHash", hash));
        log.info("Screen {}:{} saved (hash {})", request.merchantId(), request.screenKey(), hash);
        return response(Constants.MSG_SCREEN_SAVED, screen, false);
    }

    private static SaveScreenResponse response(String message, SduiScreen screen, boolean unchanged) {
        return SaveScreenResponseBuilder.builder()
                .message(message)
                .key(screen.getScreenKey())
                .lockVersion(screen.getVersion())
                .unchanged(unchanged)
                .build();
    }
}
