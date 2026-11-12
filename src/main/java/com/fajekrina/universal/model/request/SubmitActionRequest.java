package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

@RecordBuilder
public record SubmitActionRequest(String action, Map<String, Object> data, Map<String, Object> params) {}
