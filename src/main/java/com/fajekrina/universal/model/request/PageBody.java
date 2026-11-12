package com.fajekrina.universal.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;

/**
 * HTTP body of "save draft": the builder's monolith (S1). Extra fields the builder may send
 * (version, status, policy, createdAt, …) are ignored.
 *
 * @param lockVersion optimistic locking: send the lockVersion you loaded; a stale value → 409
 */
@RecordBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageBody(String pageId, String schemaVersion, Map<String, Object> layout, Integer lockVersion) { }
