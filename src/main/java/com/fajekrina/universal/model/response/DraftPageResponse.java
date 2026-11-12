package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;
import java.util.Map;

/** Draft as the builder expects it: monolith with flat parentId/order. */
@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DraftPageResponse(String pageId, String pageKey, String merchantId, String status, String schemaVersion, int version, int lockVersion, PolicyResponse policy, Map<String, Object> layout, List<Map<String, Object>> unresolvedRefs) {}
