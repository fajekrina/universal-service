package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record SaveScreenResponse(String message, String key, Integer lockVersion, boolean unchanged) {}
