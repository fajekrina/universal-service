package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActionResultResponse(String action, String receivedAt, boolean success, String message, String error, Map<String, Object> data, Map<String, Object> params, String target, String navigateTo) {}
