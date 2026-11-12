package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.request.PublishedPageRequestBuilder;
import com.fajekrina.universal.model.request.SubmitActionBody;
import com.fajekrina.universal.model.request.SubmitActionRequestBuilder;
import com.fajekrina.universal.model.response.ActionResultResponse;
import com.fajekrina.universal.model.response.SigningKeyListResponse;
import com.fajekrina.universal.model.response.SigningKeyResponse;
import com.fajekrina.universal.service.GetPublishedPageService;
import com.fajekrina.universal.service.GetSigningKeyService;
import com.fajekrina.universal.service.GetSigningKeysService;
import com.fajekrina.universal.service.SubmitActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client API (A1: assembled on the server). Serves the frozen release payload byte-for-byte
 * with ETag = release content hash, optionally as a signed envelope.
 */
@RestController
@RequestMapping(path = "universal/v1/client", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UniversalClientController {

    private final GetPublishedPageService getPublishedPageService;
    private final GetSigningKeysService getSigningKeysService;
    private final GetSigningKeyService getSigningKeyService;
    private final SubmitActionService submitActionService;

    @GetMapping("page/{merchantId}/{pageKey}")
    public ResponseEntity<Object> getPublishedPage(@PathVariable String merchantId, @PathVariable String pageKey,
                                                   @RequestParam(required = false, defaultValue = "false") boolean signed,
                                                   @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        var response = getPublishedPageService.execute(PublishedPageRequestBuilder.builder()
                .merchantId(merchantId).pageKey(pageKey).signed(signed).ifNoneMatch(ifNoneMatch).build());
        // release payloads are immutable: clients may cache but must revalidate to see new releases
        var cache = CacheControl.noCache().cachePrivate();
        if (response.notModified()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(response.contentHash()).cacheControl(cache).build();
        }
        var ok = ResponseEntity.ok().eTag(response.contentHash()).cacheControl(cache).contentType(MediaType.APPLICATION_JSON);
        return response.envelope() != null ? ok.body(response.envelope()) : ok.body(response.payload());
    }

    @GetMapping("signing-keys")
    public SigningKeyListResponse getSigningKeys() {
        return getSigningKeysService.execute();
    }

    /** @deprecated single-key endpoint kept for 0.2 clients; use signing-keys. */
    @Deprecated
    @GetMapping("signing-key")
    public SigningKeyResponse getSigningKey() {
        return getSigningKeyService.execute();
    }

    @PostMapping("action/submit")
    public ResponseEntity<ActionResultResponse> submitAction(@RequestBody SubmitActionBody body) {
        var result = submitActionService.execute(SubmitActionRequestBuilder.builder()
                .action(body.action()).data(body.data()).params(body.params()).build());
        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(result);
    }
}
