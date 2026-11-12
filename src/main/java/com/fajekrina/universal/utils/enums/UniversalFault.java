package com.fajekrina.universal.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** Business faults of universal-service: errorCode is rendered as SDUI-{errorCode:000}. */
@Getter
@RequiredArgsConstructor
public enum UniversalFault {
    PAGE_NOT_FOUND("Universal page not found", 1, HttpStatus.NOT_FOUND),
    PUBLISHED_PAGE_NOT_FOUND("Published universal page not found", 2, HttpStatus.NOT_FOUND),
    INVALID_PAYLOAD("Invalid SDUI payload", 3, HttpStatus.BAD_REQUEST),
    LOCK_CONFLICT("Page was changed by someone else", 4, HttpStatus.CONFLICT),
    UNRESOLVED_REFERENCE("Unresolved references in release", 5, HttpStatus.BAD_REQUEST),
    RELEASE_NOT_FOUND("Release not found", 6, HttpStatus.NOT_FOUND),
    TREE_NOT_FOUND("Tree version not found", 7, HttpStatus.NOT_FOUND),
    STYLE_NOT_FOUND("Style not found", 8, HttpStatus.NOT_FOUND),
    INVALID_STYLE("Invalid style artefact", 9, HttpStatus.BAD_REQUEST),
    INVALID_POLICY("Invalid policy", 10, HttpStatus.BAD_REQUEST),
    NO_DRAFT("Page has no saved draft — save before publishing", 11, HttpStatus.BAD_REQUEST),
    SIGNING_DISABLED("Signing is disabled on this server", 12, HttpStatus.BAD_REQUEST),
    SIGNING_FAILED("Unable to sign payload", 13, HttpStatus.INTERNAL_SERVER_ERROR),
    POLICY_NOT_FOUND("Merchant has no policy", 14, HttpStatus.INTERNAL_SERVER_ERROR),
    ACTION_REQUIRED("action is required", 15, HttpStatus.BAD_REQUEST),
    INTEGRITY_VIOLATION("Data integrity violation", 16, HttpStatus.CONFLICT),
    SCREEN_NOT_FOUND("Screen not found", 17, HttpStatus.NOT_FOUND),
    PUBLISHED_SCREEN_NOT_FOUND("Published screen not found", 18, HttpStatus.NOT_FOUND),
    INVALID_SCREEN("Invalid spec screen payload", 19, HttpStatus.BAD_REQUEST),
    BAD_REQUEST("Invalid or malformed request", 98, HttpStatus.BAD_REQUEST),
    GENERAL_ERROR("Unexpected error", 99, HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final int errorCode;
    private final HttpStatus status;
}
