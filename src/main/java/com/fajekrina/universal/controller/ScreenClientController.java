package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.request.PublishedScreenRequestBuilder;
import com.fajekrina.universal.model.request.ScreenManifestRequestBuilder;
import com.fajekrina.universal.model.response.PublishedPageResponse;
import com.fajekrina.universal.service.GetPublishedScreenService;
import com.fajekrina.universal.service.GetScreenManifestService;
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

/**
 * Client API for the @fajekrina/sdui engine: a screen's frozen release bytes and the version
 * manifest — bare (development) or as signed Ed25519 envelopes ({@code ?signed=true}).
 * The bare body is the stored bytes verbatim, so its sha256 is the ETag.
 */
@RestController
@RequestMapping(path = "universal/v1/client", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ScreenClientController {

    private final GetPublishedScreenService getPublishedScreenService;
    private final GetScreenManifestService getScreenManifestService;

    @GetMapping("screen/{merchantId}/{screenKey}")
    public ResponseEntity<Object> getScreen(@PathVariable String merchantId, @PathVariable String screenKey,
                                            @RequestParam(required = false, defaultValue = "false") boolean signed,
                                            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return respond(getPublishedScreenService.execute(PublishedScreenRequestBuilder.builder()
                .merchantId(merchantId).screenKey(screenKey).signed(signed).ifNoneMatch(ifNoneMatch).build()));
    }

    @GetMapping("manifest/{merchantId}")
    public ResponseEntity<Object> getManifest(@PathVariable String merchantId,
                                              @RequestParam(required = false, defaultValue = "false") boolean signed,
                                              @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return respond(getScreenManifestService.execute(ScreenManifestRequestBuilder.builder()
                .merchantId(merchantId).signed(signed).ifNoneMatch(ifNoneMatch).build()));
    }

    static ResponseEntity<Object> respond(PublishedPageResponse response) {
        // releases are immutable: clients may cache but must revalidate to see new ones
        var cache = CacheControl.noCache().cachePrivate();
        if (response.notModified()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(response.contentHash()).cacheControl(cache).build();
        }
        var ok = ResponseEntity.ok().eTag(response.contentHash()).cacheControl(cache).contentType(MediaType.APPLICATION_JSON);
        return response.envelope() != null ? ok.body(response.envelope()) : ok.body(response.payload());
    }
}
