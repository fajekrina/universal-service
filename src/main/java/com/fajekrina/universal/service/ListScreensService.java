package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiScreen;
import com.fajekrina.universal.model.entity.SduiScreenRelease;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.MerchantRequest;
import com.fajekrina.universal.model.response.ScreenListResponse;
import com.fajekrina.universal.model.response.ScreenListResponseBuilder;
import com.fajekrina.universal.model.response.ScreenListResponseItemBuilder;
import com.fajekrina.universal.repository.SduiScreenReleaseRepository;
import com.fajekrina.universal.repository.SduiScreenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Screens of a merchant (archived excluded) with status, active sequence and lock version. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListScreensService {

    private final SduiScreenRepository screenRepository;
    private final SduiScreenReleaseRepository releaseRepository;

    public ScreenListResponse execute(MerchantRequest request) {
        var screens = screenRepository.findAllByMerchantIdAndStatusNotOrderByScreenKey(request.merchantId(), PageStatus.ARCHIVED);
        Map<Long, Integer> sequences = releaseRepository.findAllByReleaseIdIn(
                        screens.stream().map(SduiScreen::getActiveReleaseId).filter(Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(SduiScreenRelease::getReleaseId, SduiScreenRelease::getSequence));
        return ScreenListResponseBuilder.builder()
                .merchantId(request.merchantId())
                .screens(screens.stream().map(s -> ScreenListResponseItemBuilder.builder()
                        .key(s.getScreenKey())
                        .status(s.getStatus().name())
                        .sequence(s.getActiveReleaseId() == null ? null : sequences.get(s.getActiveReleaseId()))
                        .lockVersion(s.getVersion())
                        .build()).toList())
                .build();
    }
}
