package com.fajekrina.universal.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.util.List;

@RecordBuilder
public record ScreenListResponse(String merchantId, List<Item> screens) {

    @RecordBuilder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(String key, String status, Integer sequence, Integer lockVersion) {}
}
