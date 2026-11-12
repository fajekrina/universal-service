package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;

@RecordBuilder
public record PolicyResponse(String merchantId, String policyVersion, List<String> allowedDomains, List<String> allowedEvents) {}
