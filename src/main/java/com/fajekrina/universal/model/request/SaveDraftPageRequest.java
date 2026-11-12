package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

/** S1 save: the monolith to split into tree + script artefacts. */
@RecordBuilder
public record SaveDraftPageRequest(String merchantId, String pageKey, String pageId, String schemaVersion, Map<String, Object> layout, Integer lockVersion) {}
