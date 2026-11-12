package com.fajekrina.universal.model.request;

import io.soabase.recordbuilder.core.RecordBuilder;

/** {@code payload}: the spec screen ({schemaVersion, screen}); {@code lockVersion}: optimistic lock of an existing draft. */
@RecordBuilder
public record SaveScreenRequest(String merchantId, String screenKey, Object payload, Integer lockVersion) {}
