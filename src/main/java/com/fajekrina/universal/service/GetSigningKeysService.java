package com.fajekrina.universal.service;

import com.fajekrina.universal.model.response.SigningKeyListResponse;
import com.fajekrina.universal.model.response.SigningKeyListResponseBuilder;
import com.fajekrina.universal.model.response.SigningKeyListResponseKeyBuilder;
import com.fajekrina.universal.repository.SduiSigningKeyRepository;
import com.fajekrina.universal.utils.constants.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All non-revoked verification keys (raw 32-byte Ed25519, base64). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSigningKeysService {

    private final SduiSigningKeyRepository signingKeyRepository;

    public SigningKeyListResponse execute() {
        var keys = signingKeyRepository.findAllByStatusNotOrderByCreatedTime(Constants.SIGNING_KEY_REVOKED).stream()
                .map(k -> SigningKeyListResponseKeyBuilder.builder()
                        .keyId(k.getKeyId())
                        .alg(Constants.SIGNING_ALGORITHM)
                        .publicKey(k.getPublicKey())
                        .status(k.getStatus())
                        .build())
                .toList();
        return SigningKeyListResponseBuilder.builder().keys(keys).build();
    }
}
