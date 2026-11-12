package com.fajekrina.universal.utils.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
    public static final String SERVICE_PREFIX = "SDUI";
    public static final String ACTOR_HEADER = "X-SDUI-Actor";
    public static final String DEFAULT_ACTOR = "builder";
    public static final String SYSTEM_ACTOR = "system";
    public static final String SEED_ACTOR = "seed";
    public static final String DEFAULT_SCHEMA_VERSION = "1.0";
    public static final String DEFAULT_LAYOUT_TYPE = "Container";
    public static final String MERCHANT_ACTIVE = "ACTIVE";
    public static final String SIGNING_ALGORITHM = "ed25519";
    public static final String SIGNING_KEY_ACTIVE = "ACTIVE";
    public static final String SIGNING_KEY_REVOKED = "REVOKED";
    public static final String ACTION_PREFIX = "a_";
    public static final String SCRIPT_SUFFIX = "-logic";
    public static final String EMPTY_JSON_ARRAY = "[]";
    public static final String MSG_PAGE_SAVED = "Universal page saved";
    public static final String MSG_PAGE_PUBLISHED = "Universal page published";
    public static final String MSG_PAGE_ROLLED_BACK = "Universal page rolled back";
    public static final String MSG_PAGE_ARCHIVED = "Universal page archived";
    public static final String MSG_POLICY_SAVED = "Universal policy saved";
    public static final String SOURCE_UNIVERSAL = "universal";
    public static final String MSG_SCREEN_SAVED = "Screen saved";
    public static final String MSG_SCREEN_UNCHANGED = "Screen unchanged";
    public static final String MSG_SCREEN_PUBLISHED = "Screen published";
    public static final String MSG_SCREEN_ALREADY_PUBLISHED = "Screen already published";
    /** audit_log.page_id of a spec screen */
    public static final String SCREEN_AUDIT_PREFIX = "screen:";
    /** manifest version of a screen release: "v" + sequence */
    public static final String SCREEN_VERSION_PREFIX = "v";
    /** schemaVersion of the manifest envelope (the spec the client speaks) */
    public static final String SPEC_SCHEMA_VERSION = "1.1";
}
