package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiScript;
import com.fajekrina.universal.model.entity.SduiTree;
import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.response.ReleaseListResponse;
import com.fajekrina.universal.model.response.ReleaseListResponseBuilder;
import com.fajekrina.universal.model.response.ReleaseListResponseReleaseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiReleaseRepository;
import com.fajekrina.universal.repository.SduiScriptRepository;
import com.fajekrina.universal.repository.SduiStyleRepository;
import com.fajekrina.universal.repository.SduiTreeRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Release table of a page (newest first), with the artefact versions each release locked. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListReleasesService {

    private final SduiPageRepository pageRepository;
    private final SduiReleaseRepository releaseRepository;
    private final SduiTreeRepository treeRepository;
    private final SduiScriptRepository scriptRepository;
    private final SduiStyleRepository styleRepository;

    public ReleaseListResponse execute(PageKeyRequest request) {
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        var releases = releaseRepository.findAllByPageIdOrderBySequenceDesc(page.getPageId()).stream().map(r -> {
            var style = r.getStyleId() == null ? null : styleRepository.findById(r.getStyleId()).orElse(null);
            return ReleaseListResponseReleaseBuilder.builder()
                    .releaseId(r.getReleaseId())
                    .sequence(r.getSequence())
                    .isActive(Objects.equals(page.getActiveReleaseId(), r.getReleaseId()))
                    .publishedAt(r.getPublishedAt())
                    .publishedBy(r.getPublishedBy())
                    .contentHash(r.getContentHash())
                    .rollbackOf(r.getRollbackOf())
                    .policyVersion(r.getPolicyVersion())
                    .signingKeyId(r.getSigningKeyId())
                    .treeVersion(treeRepository.findById(r.getTreeId()).map(SduiTree::getVersion).orElse(null))
                    .scriptVersion(r.getScriptId() == null ? null : scriptRepository.findById(r.getScriptId()).map(SduiScript::getVersion).orElse(null))
                    .styleKey(style == null ? null : style.getStyleKey())
                    .styleVersion(style == null ? null : style.getVersion())
                    .build();
        }).toList();
        return ReleaseListResponseBuilder.builder().releases(releases).build();
    }
}
