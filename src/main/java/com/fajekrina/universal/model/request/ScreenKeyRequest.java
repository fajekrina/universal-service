package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record ScreenKeyRequest(String merchantId, String screenKey) {}
