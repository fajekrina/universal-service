package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record PublishedScreenRequest(String merchantId, String screenKey, boolean signed, String ifNoneMatch) {}
