package com.fajekrina.universal.model.response;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RecordBuilder
public record ComponentTypesResponse(Map<String, ComponentType> components, Set<String> behaviors) {

    @RecordBuilder
    public record ComponentType(String category, boolean acceptsChildren, List<String> allowedBehaviors, Map<String, Object> props) { }
}
