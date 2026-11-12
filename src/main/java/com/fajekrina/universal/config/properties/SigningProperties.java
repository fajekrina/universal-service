package com.fajekrina.universal.config.properties;

import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Optional Ed25519 signing. Without key files the service stays disabled and signing fails closed.
 * Keys: base64 DER text files (PKCS8 private, X.509 public) or PEM.
 */
@RecordBuilder
@ConfigurationProperties("sdui.signing")
public record SigningProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String privateKeyFile,
        @DefaultValue("") String publicKeyFile
) { }
