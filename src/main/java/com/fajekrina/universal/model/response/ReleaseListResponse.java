package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.ZonedDateTime;
import java.util.List;

@RecordBuilder
public record ReleaseListResponse(List<Release> releases) {

    @RecordBuilder
    public record Release(Long releaseId, int sequence, boolean isActive, ZonedDateTime publishedAt, String publishedBy,
                          String contentHash, Long rollbackOf, int policyVersion, String signingKeyId,
                          Integer treeVersion, Integer scriptVersion, String styleKey, Integer styleVersion) { }
}
