package com.fajekrina.universal.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON helpers with fixed, documented output so hashes and signatures are reproducible.
 * <ul>
 *   <li>{@link #write} — compact, insertion order (frozen payloads: what is signed is what is served)</li>
 *   <li>{@link #canonical} — compact, keys sorted (artefact content_hash / dedup)</li>
 * </ul>
 */
public final class CanonicalJson {

    private static final ObjectMapper COMPACT = JsonMapper.builder().build();
    private static final ObjectMapper SORTED = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private CanonicalJson() {}

    public static String write(Object value) {
        try {
            return COMPACT.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public static String canonical(Object value) {
        try {
            return SORTED.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public static Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return COMPACT.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("stored JSON is not an object", e);
        }
    }

    public static List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return COMPACT.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("stored JSON is not a string array", e);
        }
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
