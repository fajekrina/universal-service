package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

/** The editable draft: {@code payload} is the spec screen; send {@code lockVersion} back on save. */
@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScreenDraftResponse(String merchantId, String key, String status, Integer lockVersion, Integer activeSequence,
                                  Map<String, Object> payload) {}
