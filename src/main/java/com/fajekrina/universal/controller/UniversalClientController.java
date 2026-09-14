package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.UniversalActionRequest;
import com.fajekrina.universal.model.UniversalPageConfig;
import com.fajekrina.universal.service.UniversalPageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/universal/client")
public class UniversalClientController {

    private final UniversalPageConfigService service;

    @Autowired
    public UniversalClientController(UniversalPageConfigService service) {
        this.service = service;
    }

    @GetMapping("/page/{merchantId}/{pageKey}")
    public ResponseEntity<?> getPublishedPage(@PathVariable String merchantId, @PathVariable String pageKey) {
        try {
            UniversalPageConfig config = service.getPublishedPage(merchantId, pageKey);
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Published universal page not found: " + pageKey + ". It may not exist or is not published."));
            }
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to load universal page: " + e.getMessage()));
        }
    }

    @PostMapping("/action/submit")
    public ResponseEntity<?> submitAction(@RequestBody(required = false) UniversalActionRequest request) {
        try {
            if (request == null) return ResponseEntity.badRequest().body(Map.of("error", "ActionRequest body is required"));
            if (request.getAction() == null || request.getAction().isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "action is required"));
            Map<String, Object> result = service.handleAction(request.getAction(), request.getData(), request.getParams());
            boolean success = Boolean.TRUE.equals(result.get("success"));
            return ResponseEntity.status(success ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to process universal action: " + e.getMessage()));
        }
    }
}
