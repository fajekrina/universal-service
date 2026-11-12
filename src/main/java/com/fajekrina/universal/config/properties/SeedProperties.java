package com.fajekrina.universal.config.properties;

import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Seed an EMPTY database from classpath data/universal/{policies,pages} (save + publish). */
@RecordBuilder
@ConfigurationProperties("sdui.seed")
public record SeedProperties(@DefaultValue("true") boolean enabled) { }
