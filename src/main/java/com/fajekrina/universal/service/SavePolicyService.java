package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiMerchant;
import com.fajekrina.universal.model.entity.SduiPolicy;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.request.SavePolicyRequest;
import com.fajekrina.universal.model.response.SavePolicyResponse;
import com.fajekrina.universal.model.response.SavePolicyResponseBuilder;
import com.fajekrina.universal.repository.SduiMerchantRepository;
import com.fajekrina.universal.repository.SduiPolicyRepository;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Store a new policy version (old versions are never updated). */
@Service
@Transactional
@RequiredArgsConstructor
public class SavePolicyService {

    private final SduiMerchantRepository merchantRepository;
    private final SduiPolicyRepository policyRepository;
    private final MerchantHelperService merchantHelperService;
    private final AuditHelperService auditHelperService;

    public SavePolicyResponse execute(SavePolicyRequest request) {
        if (request.body() == null) throw new UniversalException(UniversalFault.INVALID_POLICY, "policy body is required");
        List<String> domains = strings(request.body().get("allowedDomains"), "allowedDomains");
        List<String> events = strings(request.body().get("allowedEvents"), "allowedEvents");
        if (!merchantRepository.existsById(request.merchantId())) {
            merchantRepository.saveAndFlush(SduiMerchant.builder()
                    .merchantId(request.merchantId()).name(request.merchantId()).status(Constants.MERCHANT_ACTIVE).build());
        }
        int next = merchantHelperService.latestPolicy(request.merchantId()).map(p -> p.getPolicyVersion() + 1).orElse(1);
        SduiPolicy saved = policyRepository.saveAndFlush(SduiPolicy.builder()
                .merchantId(request.merchantId())
                .policyVersion(next)
                .allowedDomains(domains)
                .allowedEvents(events)
                .boundsVersion(merchantHelperService.latestBoundsVersion())
                .build());
        auditHelperService.record(request.merchantId(), null, AuditEvent.POLICY_SAVE, null, Map.of("policyVersion", next));
        return SavePolicyResponseBuilder.builder()
                .message(Constants.MSG_POLICY_SAVED)
                .policy(merchantHelperService.toResponse(saved))
                .build();
    }

    private static List<String> strings(Object value, String field) {
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new UniversalException(UniversalFault.INVALID_POLICY, field + " must be an array of strings");
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof String s) || s.isBlank()) throw new UniversalException(UniversalFault.INVALID_POLICY, field + " entries must be non-blank strings");
            out.add(s.trim());
        }
        return out;
    }
}
