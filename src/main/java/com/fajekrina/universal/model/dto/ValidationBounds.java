package com.fajekrina.universal.model.dto;

import io.soabase.recordbuilder.core.RecordBuilder;

/** Snapshot of sdui_bounds used by the save-time validator. */
@RecordBuilder
public record ValidationBounds(int maxDepth, int maxNodes, int maxStringLength, int maxProps, int maxChildren,
                               int maxArrayItems, int maxIdentifierLength, int maxChainDepth) {
    public static final ValidationBounds DEFAULT = new ValidationBounds(32, 5000, 4096, 64, 256, 512, 128, 4);
}
