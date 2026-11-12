package com.fajekrina.universal.model.dto;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.Map;
import java.util.Set;

/** Snapshot of sdui_component_type + sdui_behavior used by the save-time validator. */
@RecordBuilder
public record ComponentRegistry(Map<String, Set<String>> allowedBehaviorsByType, Set<String> containerTypes, Set<String> behaviors) {
    public Set<String> types() {
        return allowedBehaviorsByType.keySet();
    }
}
