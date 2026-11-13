package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
/** @param sequence a specific (immutable) release; {@code null} = the active release */
public record PublishedScreenRequest(String merchantId, String screenKey, Integer sequence, boolean signed, String ifNoneMatch) {}
