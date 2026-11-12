package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.request.GetStyleRequestBuilder;
import com.fajekrina.universal.model.request.GetTreeRequestBuilder;
import com.fajekrina.universal.model.request.MerchantRequestBuilder;
import com.fajekrina.universal.model.request.PageBody;
import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.request.PageKeyRequestBuilder;
import com.fajekrina.universal.model.request.RollbackPageRequestBuilder;
import com.fajekrina.universal.model.request.SaveDraftPageRequestBuilder;
import com.fajekrina.universal.model.request.SavePolicyRequestBuilder;
import com.fajekrina.universal.model.request.SaveStyleRequestBuilder;
import com.fajekrina.universal.model.request.SetPageStyleRequestBuilder;
import com.fajekrina.universal.model.response.ArchivePageResponse;
import com.fajekrina.universal.model.response.AuditListResponse;
import com.fajekrina.universal.model.response.ComponentTypesResponse;
import com.fajekrina.universal.model.response.DraftPageResponse;
import com.fajekrina.universal.model.response.PageListResponse;
import com.fajekrina.universal.model.response.PolicyResponse;
import com.fajekrina.universal.model.response.ReleaseListResponse;
import com.fajekrina.universal.model.response.ReleaseResponse;
import com.fajekrina.universal.model.response.SaveDraftPageResponse;
import com.fajekrina.universal.model.response.SavePolicyResponse;
import com.fajekrina.universal.model.response.StyleResponse;
import com.fajekrina.universal.model.response.TreeListResponse;
import com.fajekrina.universal.model.response.TreeResponse;
import com.fajekrina.universal.service.ArchivePageService;
import com.fajekrina.universal.service.GetComponentTypesService;
import com.fajekrina.universal.service.GetDraftPageService;
import com.fajekrina.universal.service.GetPageAuditService;
import com.fajekrina.universal.service.GetPolicyService;
import com.fajekrina.universal.service.GetStyleService;
import com.fajekrina.universal.service.GetTreeService;
import com.fajekrina.universal.service.ListPagesService;
import com.fajekrina.universal.service.ListReleasesService;
import com.fajekrina.universal.service.ListTreesService;
import com.fajekrina.universal.service.PublishPageService;
import com.fajekrina.universal.service.RollbackPageService;
import com.fajekrina.universal.service.SaveDraftPageService;
import com.fajekrina.universal.service.SavePolicyService;
import com.fajekrina.universal.service.SaveStyleService;
import com.fajekrina.universal.service.SetPageStyleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Merchant / builder API — one endpoint, one use-case service.
 * Actor: optional header {@code X-SDUI-Actor} (default "builder"), recorded in created_by and the audit log.
 */
@RestController
@RequestMapping(path = "universal/v1/merchant", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UniversalMerchantController {

    private final ListPagesService listPagesService;
    private final GetDraftPageService getDraftPageService;
    private final SaveDraftPageService saveDraftPageService;
    private final PublishPageService publishPageService;
    private final RollbackPageService rollbackPageService;
    private final ArchivePageService archivePageService;
    private final ListTreesService listTreesService;
    private final GetTreeService getTreeService;
    private final GetPageAuditService getPageAuditService;
    private final SetPageStyleService setPageStyleService;
    private final ListReleasesService listReleasesService;
    private final GetStyleService getStyleService;
    private final SaveStyleService saveStyleService;
    private final GetComponentTypesService getComponentTypesService;
    private final GetPolicyService getPolicyService;
    private final SavePolicyService savePolicyService;

    private static PageKeyRequest pageKey(String merchantId, String pageKey) {
        return PageKeyRequestBuilder.builder().merchantId(merchantId).pageKey(pageKey).build();
    }

    // ---------------------------------------------------------------- pages
    @GetMapping("pages/{merchantId}")
    public PageListResponse listPages(@PathVariable String merchantId) {
        return listPagesService.execute(MerchantRequestBuilder.builder().merchantId(merchantId).build());
    }

    @GetMapping("pages/{merchantId}/{pageKey}")
    public DraftPageResponse getDraftPage(@PathVariable String merchantId, @PathVariable String pageKey) {
        return getDraftPageService.execute(pageKey(merchantId, pageKey));
    }

    @PostMapping("pages/{merchantId}/{pageKey}")
    public SaveDraftPageResponse saveDraftPage(@PathVariable String merchantId, @PathVariable String pageKey,
                                               @RequestBody(required = false) PageBody body) {
        var request = SaveDraftPageRequestBuilder.builder()
                .merchantId(merchantId)
                .pageKey(pageKey)
                .pageId(body == null ? null : body.pageId())
                .schemaVersion(body == null ? null : body.schemaVersion())
                .layout(body == null ? null : body.layout())
                .lockVersion(body == null ? null : body.lockVersion())
                .build();
        return saveDraftPageService.execute(request);
    }

    @PostMapping("pages/{merchantId}/{pageKey}/publish")
    public ReleaseResponse publishPage(@PathVariable String merchantId, @PathVariable String pageKey) {
        return publishPageService.execute(pageKey(merchantId, pageKey));
    }

    @PostMapping("pages/{merchantId}/{pageKey}/rollback")
    public ReleaseResponse rollbackPage(@PathVariable String merchantId, @PathVariable String pageKey, @RequestParam("to") int toSequence) {
        return rollbackPageService.execute(RollbackPageRequestBuilder.builder()
                .merchantId(merchantId).pageKey(pageKey).toSequence(toSequence).build());
    }

    /** Archive: releases stay as immutable history; the client endpoint returns 404. */
    @DeleteMapping("pages/{merchantId}/{pageKey}")
    public ArchivePageResponse archivePage(@PathVariable String merchantId, @PathVariable String pageKey) {
        return archivePageService.execute(pageKey(merchantId, pageKey));
    }

    @GetMapping("pages/{merchantId}/{pageKey}/trees")
    public TreeListResponse listTrees(@PathVariable String merchantId, @PathVariable String pageKey) {
        return listTreesService.execute(pageKey(merchantId, pageKey));
    }

    @GetMapping("pages/{merchantId}/{pageKey}/trees/{version}")
    public TreeResponse getTree(@PathVariable String merchantId, @PathVariable String pageKey, @PathVariable int version) {
        return getTreeService.execute(GetTreeRequestBuilder.builder().merchantId(merchantId).pageKey(pageKey).version(version).build());
    }

    @GetMapping("pages/{merchantId}/{pageKey}/audit")
    public AuditListResponse getPageAudit(@PathVariable String merchantId, @PathVariable String pageKey) {
        return getPageAuditService.execute(pageKey(merchantId, pageKey));
    }

    /** Body: {"styleKey": "brand-default", "version": 3?} — null styleKey detaches the style. */
    @PutMapping("pages/{merchantId}/{pageKey}/style")
    public DraftPageResponse setPageStyle(@PathVariable String merchantId, @PathVariable String pageKey,
                                          @RequestBody Map<String, Object> body) {
        Object key = body.get("styleKey");
        Object version = body.get("version");
        return setPageStyleService.execute(SetPageStyleRequestBuilder.builder()
                .merchantId(merchantId)
                .pageKey(pageKey)
                .styleKey(key == null ? null : key.toString())
                .version(version == null ? null : Integer.valueOf(version.toString()))
                .build());
    }

    @GetMapping("releases/{merchantId}/{pageKey}")
    public ReleaseListResponse listReleases(@PathVariable String merchantId, @PathVariable String pageKey) {
        return listReleasesService.execute(pageKey(merchantId, pageKey));
    }

    // --------------------------------------------------------------- styles
    @GetMapping("styles/{merchantId}/{styleKey}")
    public StyleResponse getStyle(@PathVariable String merchantId, @PathVariable String styleKey,
                                  @RequestParam(required = false) Integer version) {
        return getStyleService.execute(GetStyleRequestBuilder.builder().merchantId(merchantId).styleKey(styleKey).version(version).build());
    }

    @PutMapping("styles/{merchantId}/{styleKey}")
    public StyleResponse saveStyle(@PathVariable String merchantId, @PathVariable String styleKey,
                                   @RequestBody Map<String, Object> body) {
        return saveStyleService.execute(SaveStyleRequestBuilder.builder().merchantId(merchantId).styleKey(styleKey).body(body).build());
    }

    // ------------------------------------------------------------- registry
    @GetMapping("component-types")
    public ComponentTypesResponse getComponentTypes() {
        return getComponentTypesService.execute();
    }

    // ------------------------------------------------------------- policies
    @GetMapping("policies/{merchantId}")
    public PolicyResponse getPolicy(@PathVariable String merchantId) {
        return getPolicyService.execute(MerchantRequestBuilder.builder().merchantId(merchantId).build());
    }

    @PutMapping("policies/{merchantId}")
    public SavePolicyResponse savePolicy(@PathVariable String merchantId, @RequestBody(required = false) Map<String, Object> body) {
        return savePolicyService.execute(SavePolicyRequestBuilder.builder().merchantId(merchantId).body(body).build());
    }
}
