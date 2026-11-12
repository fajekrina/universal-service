package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiScript;
import com.fajekrina.universal.model.entity.SduiTree;
import com.fajekrina.universal.model.enums.AuditEvent;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.SaveDraftPageRequest;
import com.fajekrina.universal.model.response.SaveDraftPageResponse;
import com.fajekrina.universal.model.response.SaveDraftPageResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiScriptRepository;
import com.fajekrina.universal.repository.SduiTreeRepository;
import com.fajekrina.universal.utils.ArtifactSplitter;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.constants.Constants;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * S1 save: validate the builder monolith, split it into tree + script artefacts
 * (identical content reuses the existing version), move the draft pointers (optimistic lock).
 */
@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class SaveDraftPageService {

    private final SduiPageRepository pageRepository;
    private final SduiTreeRepository treeRepository;
    private final SduiScriptRepository scriptRepository;
    private final MerchantHelperService merchantHelperService;
    private final ValidationHelperService validationHelperService;
    private final DraftHelperService draftHelperService;
    private final AuditHelperService auditHelperService;

    public SaveDraftPageResponse execute(SaveDraftPageRequest request) {
        if (request.layout() == null) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "layout is required");
        String schemaVersion = normalizeSchemaVersion(request.schemaVersion());
        merchantHelperService.ensureMerchant(request.merchantId());
        validationHelperService.validateOrThrow(request.layout(), merchantHelperService.policyMap(request.merchantId()));

        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseGet(() -> pageRepository.saveAndFlush(SduiPage.builder()
                        .pageId(request.pageId() != null && !request.pageId().isBlank() ? request.pageId() : request.merchantId() + "-" + request.pageKey())
                        .merchantId(request.merchantId())
                        .pageKey(request.pageKey())
                        .status(PageStatus.DRAFT)
                        .build()));
        if (request.lockVersion() != null && !request.lockVersion().equals(page.getVersion())) {
            throw new UniversalException(UniversalFault.LOCK_CONFLICT,
                    "page was changed by someone else (lockVersion " + page.getVersion() + ", got " + request.lockVersion() + ")");
        }

        ArtifactSplitter.Split split = ArtifactSplitter.split(request.layout(), schemaVersion);
        SduiTree tree = saveTree(page, split, schemaVersion);
        SduiScript script = saveScript(page, split);

        List<String> unchanged = new ArrayList<>();
        if (Objects.equals(page.getDraftTreeId(), tree.getTreeId())) unchanged.add("tree");
        if (Objects.equals(page.getDraftScriptId(), script.getScriptId())) unchanged.add("script");
        if (unchanged.size() < 2 || page.getStatus() == PageStatus.ARCHIVED) {
            page.setDraftTreeId(tree.getTreeId());
            page.setDraftScriptId(script.getScriptId());
            if (page.getStatus() == PageStatus.ARCHIVED) {
                page.setStatus(PageStatus.DRAFT);
                page.setArchivedTime(null);
            }
            page = pageRepository.saveAndFlush(page);
        }
        auditHelperService.record(request.merchantId(), page.getPageId(), AuditEvent.SAVE_DRAFT, null, Map.of(
                "tree", tree.getVersion(), "script", script.getVersion(), "unchanged", unchanged, "nodes", split.nodeCount()));

        return SaveDraftPageResponseBuilder.builder()
                .message(Constants.MSG_PAGE_SAVED)
                .tree(request.pageKey() + "@" + tree.getVersion())
                .script(request.pageKey() + Constants.SCRIPT_SUFFIX + "@" + script.getVersion())
                .unchanged(unchanged)
                .nodes(split.nodeCount())
                .page(draftHelperService.toDraftResponse(page))
                .build();
    }

    private SduiTree saveTree(SduiPage page, ArtifactSplitter.Split split, String schemaVersion) {
        String hash = CanonicalJson.sha256Hex(CanonicalJson.canonical(split.tree()));
        return treeRepository.findByPageIdAndContentHash(page.getPageId(), hash).orElseGet(() -> treeRepository.saveAndFlush(SduiTree.builder()
                .pageId(page.getPageId())
                .merchantId(page.getMerchantId())
                .version(treeRepository.findFirstByPageIdOrderByVersionDesc(page.getPageId()).map(t -> t.getVersion() + 1).orElse(1))
                .body(CanonicalJson.write(split.tree()))
                .contentHash(hash)
                .schemaVersion(schemaVersion)
                .nodeCount(split.nodeCount())
                .maxDepth(split.maxDepth())
                .build()));
    }

    @SuppressWarnings("unchecked")
    private SduiScript saveScript(SduiPage page, ArtifactSplitter.Split split) {
        String hash = CanonicalJson.sha256Hex(CanonicalJson.canonical(split.script()));
        return scriptRepository.findByPageIdAndContentHash(page.getPageId(), hash).orElseGet(() -> scriptRepository.saveAndFlush(SduiScript.builder()
                .pageId(page.getPageId())
                .merchantId(page.getMerchantId())
                .version(scriptRepository.findFirstByPageIdOrderByVersionDesc(page.getPageId()).map(s -> s.getVersion() + 1).orElse(1))
                .body(CanonicalJson.write(split.script()))
                .contentHash(hash)
                .actionCount(((Map<String, Object>) split.script().get("actions")).size())
                .build()));
    }

    static String normalizeSchemaVersion(String raw) {
        if (raw == null || raw.isBlank()) return Constants.DEFAULT_SCHEMA_VERSION;
        String[] parts = raw.trim().split("\\.", -1);
        if (parts.length != 2 || !"1".equals(parts[0]) || parts[1].isEmpty()) {
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "schemaVersion must be 1.x, got '" + raw + "'");
        }
        try {
            return "1." + Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "schemaVersion must be 1.x, got '" + raw + "'");
        }
    }
}
