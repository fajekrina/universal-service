package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record PublishedPageRequest(String merchantId, String pageKey, boolean signed, String ifNoneMatch) {}
