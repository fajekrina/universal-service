package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;

@RecordBuilder
public record SaveDraftPageResponse(String message, String tree, String script, List<String> unchanged, int nodes, DraftPageResponse page) {}
