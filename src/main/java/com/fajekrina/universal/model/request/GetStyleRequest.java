package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record GetStyleRequest(String merchantId, String styleKey, Integer version) {}
