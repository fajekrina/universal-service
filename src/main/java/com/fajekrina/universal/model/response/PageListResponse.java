package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;

@RecordBuilder
public record PageListResponse(String merchantId, List<String> pages, int count, String source) {}
