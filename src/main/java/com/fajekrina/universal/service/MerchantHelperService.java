package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiMerchant;
import com.fajekrina.universal.model.entity.SduiPolicy;
import com.fajekrina.universal.model.response.PolicyResponse;
import com.fajekrina.universal.model.response.PolicyResponseBuilder;
import com.fajekrina.universal.repository.SduiBoundsRepository;
import com.fajekrina.universal.repository.SduiMerchantRepository;
import com.fajekrina.universal.repository.SduiPolicyRepository;
import com.fajekrina.universal.utils.constants.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Merchant + policy lookups shared by several use cases. */
@Service
@RequiredArgsConstructor
public class MerchantHelperService {

    private final SduiMerchantRepository merchantRepository;
    private final SduiPolicyRepository policyRepository;
    private final SduiBoundsRepository boundsRepository;

    /** Make sure the merchant row and a v1 (empty) policy exist. */
    @Transactional
    public void ensureMerchant(String merchantId) {
        if (!merchantRepository.existsById(merchantId)) {
            merchantRepository.saveAndFlush(SduiMerchant.builder()
                    .merchantId(merchantId)
                    .name(merchantId)
                    .status(Constants.MERCHANT_ACTIVE)
                    .build());
        }
        if (policyRepository.findFirstByMerchantIdOrderByPolicyVersionDesc(merchantId).isEmpty()) {
            policyRepository.saveAndFlush(SduiPolicy.builder()
                    .merchantId(merchantId)
                    .policyVersion(1)
                    .allowedDomains(List.of())
                    .allowedEvents(List.of())
                    .boundsVersion(latestBoundsVersion())
                    .build());
        }
    }

    public Optional<SduiPolicy> latestPolicy(String merchantId) {
        return policyRepository.findFirstByMerchantIdOrderByPolicyVersionDesc(merchantId);
    }

    public int latestBoundsVersion() {
        return boundsRepository.findFirstByOrderByBoundsVersionDesc().map(b -> b.getBoundsVersion()).orElse(1);
    }

    public PolicyResponse toResponse(SduiPolicy policy) {
        return PolicyResponseBuilder.builder()
                .merchantId(policy.getMerchantId())
                .policyVersion(String.valueOf(policy.getPolicyVersion()))
                .allowedDomains(policy.getAllowedDomains())
                .allowedEvents(policy.getAllowedEvents())
                .build();
    }

    public PolicyResponse emptyPolicy(String merchantId) {
        return PolicyResponseBuilder.builder()
                .merchantId(merchantId)
                .policyVersion("0")
                .allowedDomains(List.of())
                .allowedEvents(List.of())
                .build();
    }

    /** Policy as the validator reads it ({allowedDomains, allowedEvents}); null when absent. */
    public Map<String, Object> policyMap(String merchantId) {
        return latestPolicy(merchantId).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("allowedDomains", p.getAllowedDomains());
            m.put("allowedEvents", p.getAllowedEvents());
            return m;
        }).orElse(null);
    }
}
