package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.MerchantRequest;
import com.fajekrina.universal.model.response.PageListResponse;
import com.fajekrina.universal.model.response.PageListResponseBuilder;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.utils.constants.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Page keys of a merchant (archived pages excluded). */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListPagesService {

    private final SduiPageRepository pageRepository;

    public PageListResponse execute(MerchantRequest request) {
        List<String> pages = pageRepository.findAllByMerchantIdAndStatusNotOrderByPageKey(request.merchantId(), PageStatus.ARCHIVED)
                .stream().map(SduiPage::getPageKey).toList();
        return PageListResponseBuilder.builder()
                .merchantId(request.merchantId())
                .pages(pages)
                .count(pages.size())
                .source(Constants.SOURCE_UNIVERSAL)
                .build();
    }
}
