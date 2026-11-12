package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
/** {@code lockVersion}: the draft lock after publishing (publish touches the screen row) — send it with the next save. */
public record ScreenReleaseResponse(String message, String key, int sequence, String contentHash, String signingKeyId, boolean unchanged,
                                    Integer lockVersion) {}
