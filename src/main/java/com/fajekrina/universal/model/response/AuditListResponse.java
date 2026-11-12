package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RecordBuilder
public record AuditListResponse(List<Entry> entries) {

    @RecordBuilder
    public record Entry(String event, Long releaseId, String actor, Map<String, Object> detail, ZonedDateTime createdTime) { }
}
