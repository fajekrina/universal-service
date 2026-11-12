package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.ZonedDateTime;

@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String errorDesc, String errorCode, String errorMessage, ZonedDateTime timestamp) {}
