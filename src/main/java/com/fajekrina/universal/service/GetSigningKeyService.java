package com.fajekrina.universal.service;

import com.fajekrina.universal.model.response.SigningKeyResponse;
import com.fajekrina.universal.model.response.SigningKeyResponseBuilder;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** @deprecated single configured key (0.2 clients); use {@link GetSigningKeysService}. */
@Deprecated
@Service
@RequiredArgsConstructor
public class GetSigningKeyService {

    private final SignatureService signatureService;

    public SigningKeyResponse execute() {
        if (!signatureService.isEnabled()) throw new UniversalException(UniversalFault.SIGNING_DISABLED);
        try {
            return SigningKeyResponseBuilder.builder()
                    .alg(Constants.SIGNING_ALGORITHM)
                    .keyId(signatureService.keyId())
                    .publicKey(signatureService.publicKeyBase64())
                    .build();
        } catch (Exception e) {
            throw new UniversalException(UniversalFault.SIGNING_FAILED, e.getMessage(), e);
        }
    }
}
