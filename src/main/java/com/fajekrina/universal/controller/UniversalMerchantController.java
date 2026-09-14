package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.UniversalPageConfig;
import com.fajekrina.universal.service.UniversalPageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/universal/merchant")
public class UniversalMerchantController {

    private final UniversalPageConfigService service;

    @Autowired
    public UniversalMerchantController(UniversalPageConfigService service) {
        this.service = service;
    }

    @GetMapping("/pages/{merchantId}")
    public ResponseEntity<?> listPages(@PathVariable String merchantId) {
        try {
            List<String> pages = service.listPages(merchantId);
            return ResponseEntity.ok(Map.of(
                    "merchantId", merchantId,
                    "pages", pages,
                    "count", pages.size(),
                    "source", "universal"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to list universal pages: " + e.getMessage()));
        }
    }

    @GetMapping("/pages/{merchantId}/{pageKey}")
    public ResponseEntity<?> getPage(@PathVariable String merchantId, @PathVariable String pageKey) {
        try {
            UniversalPageConfig config = service.getPage(merchantId, pageKey);
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Universal page not found: " + pageKey));
            }
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to get universal page: " + e.getMessage()));
        }
    }

    @PostMapping("/pages/{merchantId}/{pageKey}")
    public ResponseEntity<?> savePage(@PathVariable String merchantId,
                                      @PathVariable String pageKey,
                                      @RequestBody(required = false) UniversalPageConfig pageConfig) {
        try {
            if (pageConfig == null) pageConfig = new UniversalPageConfig();
            UniversalPageConfig saved = service.savePage(merchantId, pageKey, pageConfig);
            return ResponseEntity.ok(Map.of("message", "Universal page saved", "page", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to save universal page: " + e.getMessage()));
        }
    }

    @PostMapping("/pages/{merchantId}/{pageKey}/publish")
    public ResponseEntity<?> publishPage(@PathVariable String merchantId, @PathVariable String pageKey) {
        try {
            UniversalPageConfig published = service.publishPage(merchantId, pageKey);
            if (published == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Universal page not found: " + pageKey));
            }
            return ResponseEntity.ok(Map.of("message", "Universal page published", "page", published));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to publish: " + e.getMessage()));
        }
    }

    @DeleteMapping("/pages/{merchantId}/{pageKey}")
    public ResponseEntity<?> deletePage(@PathVariable String merchantId, @PathVariable String pageKey) {
        try {
            boolean deleted = service.deletePage(merchantId, pageKey);
            if (!deleted) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Universal page not found: " + pageKey));
            return ResponseEntity.ok(Map.of("message", "Universal page deleted", "pageKey", pageKey));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete: " + e.getMessage()));
        }
    }
}
