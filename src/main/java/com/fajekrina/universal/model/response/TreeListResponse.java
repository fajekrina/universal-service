package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@RecordBuilder
public record TreeListResponse(List<TreeVersion> trees) {

    @RecordBuilder
    public record TreeVersion(Long treeId, int version, String contentHash, String schemaVersion, int nodeCount,
                              int maxDepth, String createdBy, ZonedDateTime createdTime, String note) { }
}
