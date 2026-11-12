package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record SetPageStyleRequest(String merchantId, String pageKey, String styleKey, Integer version) {}
