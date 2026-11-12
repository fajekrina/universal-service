package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record SigningKeyResponse(String alg, String keyId, String publicKey) {}
