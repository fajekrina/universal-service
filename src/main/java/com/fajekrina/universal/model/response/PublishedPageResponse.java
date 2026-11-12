package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

/** Served release: the exact payload bytes that were hashed (ETag) and signed. */
@RecordBuilder
public record PublishedPageResponse(boolean notModified, String contentHash, String payload, SignedEnvelope envelope) {

    @RecordBuilder
    public record SignedEnvelope(String schemaVersion, String alg, String keyId, String payload, String signature) { }
}
