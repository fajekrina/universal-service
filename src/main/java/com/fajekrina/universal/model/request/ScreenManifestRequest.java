package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record ScreenManifestRequest(String merchantId, boolean signed, String ifNoneMatch) {}
