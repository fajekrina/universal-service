package com.fajekrina.universal.service;

import com.fajekrina.universal.model.entity.SduiPage;
import com.fajekrina.universal.model.entity.SduiRelease;
import com.fajekrina.universal.model.enums.PageStatus;
import com.fajekrina.universal.model.request.PublishedPageRequestBuilder;
import com.fajekrina.universal.repository.SduiMerchantRepository;
import com.fajekrina.universal.repository.SduiPageRepository;
import com.fajekrina.universal.repository.SduiReleaseRepository;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPublishedPageServiceTest {

    @InjectMocks
    GetPublishedPageService getPublishedPageService;

    @Mock SduiMerchantRepository merchantRepository;
    @Mock SduiPageRepository pageRepository;
    @Mock SduiReleaseRepository releaseRepository;
    @Mock SignatureService signatureService;

    SduiRelease release;

    @BeforeEach
    void init() {
        release = SduiRelease.builder().releaseId(7L).payload("{\"pageKey\":\"home\",\"schemaVersion\":\"1.0\"}").contentHash("abc").build();
        lenient().when(merchantRepository.existsByMerchantIdAndStatus("m", "ACTIVE")).thenReturn(true);
        lenient().when(pageRepository.findByMerchantIdAndPageKey("m", "home")).thenReturn(Optional.of(
                SduiPage.builder().pageId("m-home").status(PageStatus.PUBLISHED).activeReleaseId(7L).build()));
        lenient().when(releaseRepository.findById(7L)).thenReturn(Optional.of(release));
    }

    private static PublishedPageRequestBuilder req() {
        return PublishedPageRequestBuilder.builder().merchantId("m").pageKey("home");
    }

    @Test
    void servesTheFrozenPayloadWithItsHash() {
        var res = getPublishedPageService.execute(req().build());
        assertThat(res.notModified()).isFalse();
        assertThat(res.payload()).isEqualTo(release.getPayload());
        assertThat(res.contentHash()).isEqualTo("abc");
    }

    @Test
    void matchingIfNoneMatchIsNotModified() {
        assertThat(getPublishedPageService.execute(req().ifNoneMatch("W/\"abc\"").build()).notModified()).isTrue();
    }

    @Test
    void signedRequestFailsWhenSigningIsOff() {
        when(signatureService.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> getPublishedPageService.execute(req().signed(true).build()))
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.SIGNING_DISABLED));
    }

    @Test
    void suspendedMerchantOrArchivedPageIsNotFound() {
        when(merchantRepository.existsByMerchantIdAndStatus("m", "ACTIVE")).thenReturn(false);
        assertThatThrownBy(() -> getPublishedPageService.execute(req().build()))
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.PUBLISHED_PAGE_NOT_FOUND));
    }
}
