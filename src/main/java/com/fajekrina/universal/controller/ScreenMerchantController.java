package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.request.MerchantRequestBuilder;
import com.fajekrina.universal.model.request.SaveScreenBody;
import com.fajekrina.universal.model.request.SaveScreenRequestBuilder;
import com.fajekrina.universal.model.request.ScreenKeyRequestBuilder;
import com.fajekrina.universal.model.response.SaveScreenResponse;
import com.fajekrina.universal.model.response.ScreenDraftResponse;
import com.fajekrina.universal.model.response.ScreenListResponse;
import com.fajekrina.universal.model.response.ScreenReleaseResponse;
import com.fajekrina.universal.service.GetScreenDraftService;
import com.fajekrina.universal.service.ListScreensService;
import com.fajekrina.universal.service.PublishScreenService;
import com.fajekrina.universal.service.SaveScreenDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Builder API for spec screens ({schemaVersion, screen} — the @fajekrina/sdui format).
 * Actor: optional header {@code X-SDUI-Actor} (default "builder"), recorded in the audit log.
 */
@RestController
@RequestMapping(path = "universal/v1/merchant/screens", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ScreenMerchantController {

    private final ListScreensService listScreensService;
    private final GetScreenDraftService getScreenDraftService;
    private final SaveScreenDraftService saveScreenDraftService;
    private final PublishScreenService publishScreenService;

    @GetMapping("{merchantId}")
    public ScreenListResponse listScreens(@PathVariable String merchantId) {
        return listScreensService.execute(MerchantRequestBuilder.builder().merchantId(merchantId).build());
    }

    @GetMapping("{merchantId}/{screenKey}")
    public ScreenDraftResponse getDraft(@PathVariable String merchantId, @PathVariable String screenKey) {
        return getScreenDraftService.execute(ScreenKeyRequestBuilder.builder().merchantId(merchantId).screenKey(screenKey).build());
    }

    /** Body: {"payload": {schemaVersion, screen}, "lockVersion": n?} — omit lockVersion for a new screen. */
    @PostMapping("{merchantId}/{screenKey}")
    public SaveScreenResponse saveDraft(@PathVariable String merchantId, @PathVariable String screenKey, @RequestBody SaveScreenBody body) {
        return saveScreenDraftService.execute(SaveScreenRequestBuilder.builder()
                .merchantId(merchantId).screenKey(screenKey).payload(body.payload()).lockVersion(body.lockVersion()).build());
    }

    @PostMapping("{merchantId}/{screenKey}/publish")
    public ScreenReleaseResponse publish(@PathVariable String merchantId, @PathVariable String screenKey) {
        return publishScreenService.execute(ScreenKeyRequestBuilder.builder().merchantId(merchantId).screenKey(screenKey).build());
    }
}
