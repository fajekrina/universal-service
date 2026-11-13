package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.request.PublishedScreenRequestBuilder;
import com.fajekrina.universal.model.request.ScreenManifestRequestBuilder;
import com.fajekrina.universal.model.response.PublishedPageResponse;
import com.fajekrina.universal.service.GetPublishedScreenService;
import com.fajekrina.universal.service.GetScreenManifestService;
import com.fajekrina.universal.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Client API for the @fajekrina/sdui engine: a screen's frozen release bytes and the version
 * manifest — as signed Ed25519 envelopes, or bare for development.
 *
 * <p>{@code ?signed} is optional: absent, responses are signed whenever signing is enabled
 * ({@code sdui.signing.enabled=true}), so {@code specScreenProvider}/{@code specManifestLoader}
 * need no query parameter. {@code ?signed=false} still returns the bare bytes.
 *
 * <ul>
 *   <li>{@code screens/{mid}/{key}/v{seq}} — one immutable release (the manifest version):
 *       {@code public, max-age=31536000, immutable}</li>
 *   <li>{@code screens/{mid}/{key}} — the active release (no manifest yet): revalidate with ETag</li>
 *   <li>{@code screen/{mid}/{key}} — the same, kept for clients of release 1.1 of this API</li>
 * </ul>
 * The bare body is the stored bytes verbatim, so its sha256 is the ETag.
 */
@RestController
@RequestMapping(path = "universal/v1/client", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ScreenClientController {

    private final GetPublishedScreenService getPublishedScreenService;
    private final GetScreenManifestService getScreenManifestService;
    private final SignatureService signatureService;

    @GetMapping({ "screens/{merchantId}/{screenKey}", "screen/{merchantId}/{screenKey}" })
    public ResponseEntity<Object> getScreen(@PathVariable String merchantId, @PathVariable String screenKey,
                                            @RequestParam(required = false) Boolean signed,
                                            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return respond(getPublishedScreenService.execute(PublishedScreenRequestBuilder.builder()
                .merchantId(merchantId).screenKey(screenKey).signed(signed(signed)).ifNoneMatch(ifNoneMatch).build()),
                CacheControl.noCache().cachePrivate());
    }

    @GetMapping("screens/{merchantId}/{screenKey}/v{sequence}")
    public ResponseEntity<Object> getScreenVersion(@PathVariable String merchantId, @PathVariable String screenKey,
                                                   @PathVariable Integer sequence,
                                                   @RequestParam(required = false) Boolean signed,
                                                   @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        // a release never changes: CDNs and browsers may keep it forever
        return respond(getPublishedScreenService.execute(PublishedScreenRequestBuilder.builder()
                .merchantId(merchantId).screenKey(screenKey).sequence(sequence).signed(signed(signed)).ifNoneMatch(ifNoneMatch).build()),
                CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }

    @GetMapping("manifest/{merchantId}")
    public ResponseEntity<Object> getManifest(@PathVariable String merchantId,
                                              @RequestParam(required = false) Boolean signed,
                                              @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return respond(getScreenManifestService.execute(ScreenManifestRequestBuilder.builder()
                .merchantId(merchantId).signed(signed(signed)).ifNoneMatch(ifNoneMatch).build()),
                CacheControl.noCache().cachePrivate());
    }

    /** {@code ?signed} when given; otherwise signed exactly when signing is enabled. */
    private boolean signed(Boolean requested) {
        return requested != null ? requested : signatureService.isEnabled();
    }

    static ResponseEntity<Object> respond(PublishedPageResponse response, CacheControl cache) {
        if (response.notModified()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(response.contentHash()).cacheControl(cache).build();
        }
        var ok = ResponseEntity.ok().eTag(response.contentHash()).cacheControl(cache).contentType(MediaType.APPLICATION_JSON);
        return response.envelope() != null ? ok.body(response.envelope()) : ok.body(response.payload());
    }
}
