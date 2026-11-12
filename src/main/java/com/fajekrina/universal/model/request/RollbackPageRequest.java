package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record RollbackPageRequest(String merchantId, String pageKey, int toSequence) {}
