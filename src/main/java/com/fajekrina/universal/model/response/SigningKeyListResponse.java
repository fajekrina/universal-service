package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;

@RecordBuilder
public record SigningKeyListResponse(List<Key> keys) {

    @RecordBuilder
    public record Key(String keyId, String alg, String publicKey, String status) { }
}
