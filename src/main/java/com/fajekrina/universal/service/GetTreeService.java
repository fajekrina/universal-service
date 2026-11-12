package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiTree;
import com.fajekrina.universal.model.request.GetTreeRequest;
import com.fajekrina.universal.model.response.TreeResponse;
import com.fajekrina.universal.model.response.TreeResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiTreeRepository;
import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One tree version with its body (diff in the builder). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetTreeService {

    private final SduiPageRepository pageRepository;
    private final SduiTreeRepository treeRepository;

    public TreeResponse execute(GetTreeRequest request) {
        SduiPage page = pageRepository.findByMerchantIdAndPageKey(request.merchantId(), request.pageKey())
                .orElseThrow(() -> new UniversalException(UniversalFault.PAGE_NOT_FOUND, "Universal page not found: " + request.pageKey()));
        SduiTree tree = treeRepository.findByPageIdAndVersion(page.getPageId(), request.version())
                .orElseThrow(() -> new UniversalException(UniversalFault.TREE_NOT_FOUND, "tree version not found: " + request.version()));
        return TreeResponseBuilder.builder()
                .treeKey(request.pageKey())
                .version(tree.getVersion())
                .contentHash(tree.getContentHash())
                .createdBy(tree.getCreatedBy())
                .createdTime(tree.getCreatedTime())
                .body(CanonicalJson.readMap(tree.getBody()))
                .build();
    }
}
