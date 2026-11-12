package com.fajekrina.universal.service;

import com.fajekrina.universal.model.request.MerchantRequest;
import com.fajekrina.universal.model.response.PolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Latest policy of a merchant (empty policy v0 when none exists). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPolicyService {

    private final MerchantHelperService merchantHelperService;

    public PolicyResponse execute(MerchantRequest request) {
        return merchantHelperService.latestPolicy(request.merchantId())
                .map(merchantHelperService::toResponse)
                .orElse(merchantHelperService.emptyPolicy(request.merchantId()));
    }
}
