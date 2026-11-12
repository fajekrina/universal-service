package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReleaseResponse(String message, String releaseId, int sequence, String contentHash, int nodes, String signingKeyId, Long rollbackOf) {}
