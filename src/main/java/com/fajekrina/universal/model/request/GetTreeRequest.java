package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record GetTreeRequest(String merchantId, String pageKey, int version) {}
