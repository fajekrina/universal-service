package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.ZonedDateTime;
import java.util.Map;

@RecordBuilder
public record TreeResponse(String treeKey, int version, String contentHash, String createdBy, ZonedDateTime createdTime, Map<String, Object> body) {}
