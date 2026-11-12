package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record ArchivePageResponse(String message, String pageKey) {}
