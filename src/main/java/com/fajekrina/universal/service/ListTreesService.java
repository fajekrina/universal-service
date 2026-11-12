package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.request.PageKeyRequest;
import com.fajekrina.universal.model.response.TreeListResponse;
import com.fajekrina.universal.model.response.TreeListResponseBuilder;
import com.fajekrina.universal.model.response.TreeListResponseTreeVersionBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiTreeRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Version history of a page's tree artefact (without bodies). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListTreesService {

    private final SduiPageRepository pageRepository;
    private final SduiTreeRepository treeRepository;

    public TreeListResponse execute(PageKeyRequest request) {
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        var trees = treeRepository.findAllByPageIdOrderByVersionDesc(page.getPageId()).stream()
                .map(t -> TreeListResponseTreeVersionBuilder.builder()
                        .treeId(t.getTreeId())
                        .version(t.getVersion())
                        .contentHash(t.getContentHash())
                        .schemaVersion(t.getSchemaVersion())
                        .nodeCount(t.getNodeCount())
                        .maxDepth(t.getMaxDepth())
                        .createdBy(t.getCreatedBy())
                        .createdTime(t.getCreatedTime())
                        .note(t.getNote())
                        .build())
                .toList();
        return TreeListResponseBuilder.builder().trees(trees).build();
    }
}
