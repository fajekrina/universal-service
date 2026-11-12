package com.fajekrina.universal.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

/** HTTP body of POST client/action/submit. */
@RecordBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubmitActionBody(String action, Map<String, Object> data, Map<String, Object> params) { }
