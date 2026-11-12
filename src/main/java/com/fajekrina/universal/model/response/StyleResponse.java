package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

@RecordBuilder
public record StyleResponse(String styleKey, int version, String ref, String contentHash, Map<String, Object> tokens, Map<String, Object> shared) {}
