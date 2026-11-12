package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

/** body = {"allowedDomains": [...], "allowedEvents": [...]} */
@RecordBuilder
public record SavePolicyRequest(String merchantId, Map<String, Object> body) {}
