package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record SavePolicyResponse(String message, PolicyResponse policy) {}
