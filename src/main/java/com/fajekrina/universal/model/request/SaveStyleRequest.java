package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

/** body = {"tokens": {category: {name: value}}, "shared": {name: css}} */
@RecordBuilder
public record SaveStyleRequest(String merchantId, String styleKey, Map<String, Object> body) {}
