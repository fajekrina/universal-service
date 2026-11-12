package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiTree;
import com.fajekrina.universal.model.response.DraftPageResponse;
import com.fajekrina.universal.model.response.DraftPageResponseBuilder;
import com.fajekrina.universal.repository.SduiScriptRepository;
import com.fajekrina.universal.repository.SduiStyleRepository;
import com.fajekrina.universal.repository.SduiTreeRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.PayloadAssembler;
import com.fajekrina.universal.utils.constants.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Draft artefacts → monolith with flat parentId/order (same assembler as the release view). */
@Service
@RequiredArgsConstructor
public class DraftHelperService {

    private final SduiTreeRepository treeRepository;
    private final SduiScriptRepository scriptRepository;
    private final SduiStyleRepository styleRepository;
    private final MerchantHelperService merchantHelperService;

    public DraftPageResponse toDraftResponse(SduiPage page) {
        SduiTree tree = treeRepository.findById(page.getDraftTreeId()).orElseThrow();
        Map<String, Object> treeBody = CanonicalJson.readMap(tree.getBody());
        Map<String, Object> scriptBody = page.getDraftScriptId() == null ? null
                : CanonicalJson.readMap(scriptRepository.findById(page.getDraftScriptId()).orElseThrow().getBody());
        Map<String, Object> styleBody = page.getDraftStyleId() == null ? null
                : CanonicalJson.readMap(styleRepository.findById(page.getDraftStyleId()).orElseThrow().getBody());
        PayloadAssembler.Result result = PayloadAssembler.flatten(treeBody, styleBody, scriptBody);

        return DraftPageResponseBuilder.builder()
                .pageId(page.getPageId())
                .pageKey(page.getPageKey())
                .merchantId(page.getMerchantId())
                .status(page.getStatus().name())
                .schemaVersion(String.valueOf(treeBody.getOrDefault("schemaVersion", Constants.DEFAULT_SCHEMA_VERSION)))
                .version(tree.getVersion())
                .lockVersion(page.getVersion() == null ? 0 : page.getVersion())
                .policy(merchantHelperService.latestPolicy(page.getMerchantId())
                        .map(merchantHelperService::toResponse)
                        .orElse(merchantHelperService.emptyPolicy(page.getMerchantId())))
                .layout(PayloadAssembler.layout(treeBody, result.components()))
                .unresolvedRefs(result.unresolved().isEmpty() ? null : result.unresolved())
                .build();
    }
}
