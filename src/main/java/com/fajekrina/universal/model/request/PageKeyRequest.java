package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record PageKeyRequest(String merchantId, String pageKey) {}
